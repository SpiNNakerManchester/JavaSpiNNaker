// Copyright (c) 2021 The University of Manchester
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/** The minimum size of a basic cell of the map. Hexes are made of several of these. */
const minMapFactor = 12;
/** The number of pixels to allow for the margin on maps. */
const mapMargin = 5;

/** Machine logical global coordinate space (x, y, z). */
type BoardTriad = readonly [number, number, number];
/** The list of canvas coordinates of a hexagon representing a board. */
type HexCoords = readonly [number, number][];
/** The type of a function to generate a key for a board. */
type BoardKeyGen = (key: BoardTriad) => string;
/** The type of a function to find the coordinates of a hex. */
type LocMapper = (t: BoardTriad) => HexCoords | undefined;

/** The various forms of board locations spat out by the server. */
interface BoardLocator {
	/** Logical board coordinates. */
	triad: {
		x: number;
		y: number;
		z: number;
	};
	/** Physical board coordinates. */
	physical: {
		cabinet: number;
		frame: number;
		board: number;
	};
	/** Network address coordinates. */
	network?: {
		address: string;
	};
};

/** Briefly describes a job on a machine. */
interface MachineJobDescriptor {
	/** The job ID. */
	id: number;
	/** The address of the job details page. */
	url?: string;
	/** The owner of the job, if the current user is allowed to see that. */
	owner?: string;
	/** The boards allocated to the job. */
	boards: BoardLocator[];
};

/** Describes a machine. */
interface MachineDescriptor {
	/** The name of the machine. */
	name: string;
	/** The width of the machine, in triads. */
	width: number;
	/** The height of the machine, in triads. */
	height: number;
	/** The number of boards currently in use. */
	num_in_use: number;
	/** The tags of the machine. */
	tags: string[];
	/** The jobs on the machine. */
	jobs: MachineJobDescriptor[];
	/** The boards that can have jobs running on them. */
	live_boards: BoardLocator[];
	/** The boards that are out of service. */
	dead_boards: BoardLocator[];
};

/** Describes a job. */
interface JobDescriptor {
	/** The job ID. */
	id: number;
	/** The owner of the job, if the current user is allowed to see that. */
	owner?: string;
	/** The state of the job. */
	state: string;
	/** The instant that the job started. */
	start: string;
	/** The keep-alive period. */
	keep_alive: string;
	/**
	 * The host that is keeping the job alive, if the current user is allowed
	 * to see that.
	 */
	owner_host?: string;
	/**
	 * The width of the job's allocation in boards, if an allocation has been
	 * made.
	 */
	width?: number;
	/**
	 * The height of the job's allocation in boards, if an allocation has been
	 * made.
	 */
	height?: number;
	/** Whether the job's allocated boards are switched on. */
	powered: boolean;
	/** What machine is the job using. */
	machine: string;
	/** Address of the machine information page. */
	machine_url: string;
	/** The list of boards allocated to the job. */
	boards: BoardLocator[];
	/**
	 * The original job request message, if the current user is allowed
	 * to see that.
	 */
	request?: any;
	/** The width of the job's allocation, in triads. */
	triad_width: number;
	/** The height of the job's allocation, in triads. */
	triad_height: number;
};

/** Type of a function for looking up the location descriptor for a board. */
type GetBoardInfo = (board: BoardTriad) => BoardLocator | undefined;
/** Type of a function for looking up the job descriptor for a board. */
type GetJobInfo = (board: BoardTriad) => MachineJobDescriptor | undefined;

/**
 * Draw a single board cell, identified in machine coordinates. This is a
 * distorted hexagon. Caller must configure colours prior to calling.
 *
 * @param ctx
 *		The drawing context.
 * @param rootX
 *		The canvas X coordinate of the bottom left of the machine.
 * @param rootY
 *		The canvas Y coordinate of the bottom left of the machine.
 * @param scale
 *		The fundamental length scale for the size of boards.
 * @param triadCoords
 *		Triad coordinates of the board to draw
 * @param fill
 *		Whether to fill the cell or just draw the outline.
 * @param labeller
 *		Function to generate the label.
 * @return
 *		The coordinates of the vertices of the cell.
 */
function drawTriadBoard(
		ctx: CanvasRenderingContext2D,
		rootX: number, rootY: number, scale: number,
		triadCoords: BoardTriad,
		fill: boolean = false,
		labeller: BoardKeyGen | undefined = undefined) : HexCoords {
	const [tx, ty, tz] = triadCoords;
	var bx : number = rootX + tx * 3 * scale;
	var by : number = rootY - ty * 3 * scale;
	if (tz == 1) {
		bx += 2 * scale;
		by -= scale;
	} else if (tz == 2) {
		bx += scale;
		by -= 2 * scale;
	}
	const label = labeller?.(triadCoords);

	/** The coordinate list. */
	const coords: [number, number][] = [];
	coords.push([bx, by]);
	coords.push([bx + scale, by]);
	coords.push([bx + 2 * scale, by - scale]);
	coords.push([bx + 2 * scale, by - 2 * scale]);
	coords.push([bx + scale, by - 2 * scale]);
	coords.push([bx, by - scale]);

	// Transfer coords into a path
	ctx.beginPath();
	coords.forEach(([x, y], i) => {
		if (i) {ctx.lineTo(x, y);} else {ctx.moveTo(x, y);}
	});
	ctx.closePath();

	if (fill) {
		ctx.fill();
	}
	ctx.stroke();
	if (label !== undefined) {
		ctx.save();
		ctx.fillStyle = ctx.strokeStyle;
		ctx.textAlign = "center";
		ctx.fillText(label, bx + scale, by - scale, 2 * scale);
		ctx.restore();
	}
	return coords;
}

/**
 * Convert board triad coordinates into a key suitable for a Map.
 *
 * @param coords
 *		The coordinates.
 * @return
 * 		The key string
 */
function tuplekey(coords: BoardTriad) : string {
	const [x, y, z] = coords;
	return x + "," + y + "," + z;
}

/**
 * Convert a list of board locators into a map using the triad coords as key.
 *
 * @param ctx
 *		How to draw on the canvas.
 * @param rootX
 *		The root X coordinate for drawing.
 * @param rootY
 *		The root Y coordinate for drawing.
 * @param scale
 *		The diagram basic scaling factor.
 * @param width
 *		The width of the diagram, in triads.
 * @param height
 *		The height of the diagram, in triads.
 * @param depth
 *		The depth of the diagram (usually 1 or 3).
 * @param colourer
 *		How to get a colour for a board.
 * @param labeller
 *		How to get the label for a board.
 * @return
 *		Description of where each board was drawn.
 */
function drawLayout(
		ctx: CanvasRenderingContext2D,
		rootX: number, rootY: number, scale: number,
		width: number, height: number, depth: number = 3,
		colourer: string | BoardKeyGen | undefined = undefined,
		labeller: BoardKeyGen | undefined = undefined) :
			Map<string,[BoardTriad,HexCoords]> {
	const tloc : Map<string,[BoardTriad,HexCoords]> = new Map();
	for (var y : number = 0; y < height; y++) {
		for (var x : number = 0; x < width; x++) {
			for (var z : number = 0; z < depth; z++) {
				const key = [x, y, z] as const;
				if (typeof colourer === 'string') {
					ctx.fillStyle = colourer;
				} else if (colourer !== undefined) {
					ctx.fillStyle = colourer(key);
				}
				tloc.set(tuplekey(key),[key, drawTriadBoard(
						ctx, rootX, rootY, scale, key, true, labeller)]);
			}
		}
	}
	return tloc;
}

/**
 * Test if a point is inside a board.
 *
 * @param x
 *		Canvas X coordinate
 * @param y
 *		Canvas Y coordinate
 * @param tloc
 *		Location mapping from `drawLayout()`
 * @return
 *		Which board the point is in, or `undefined` if none.
 */
// https://stackoverflow.com/a/29915728/301832
function inside(
		x: number, y: number,
		tloc: Map<string,[BoardTriad,HexCoords]>) : BoardTriad | undefined {
    // ray-casting algorithm based on
    // https://wrf.ecse.rpi.edu/Research/Short_Notes/pnpoly.html/pnpoly.html

	for (const item of tloc) {
		const [triad, poly] = item[1];
	    var inside = false;
	    for (var i = 0, j = poly.length - 1; i < poly.length; j = i++) {
	        const [xi, yi] = poly[i];
	        const [xj, yj] = poly[j];

	        const intersect = ((yi > y) != (yj > y))
	            && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
	        if (intersect) {
				inside = !inside;
			}
	    }
		if (inside) {
			return triad;
		}
	}

    return undefined;
}

/**
 * Convert a list of board locators into a map using the triad coords as key.
 *
 * @param boards
 *		The board information list.
 * @return
 *		Map from triad coords to full locators.
 */
function boardMap(boards: readonly BoardLocator[]) : Map<string,BoardLocator> {
	const m : Map<string,BoardLocator> = new Map();
	for (const b of boards) {
		const {x:x, y:y, z:z} = b.triad;
		m.set(tuplekey([x,y,z]), b);
	}
	return m;
}

/**
 * Convert a list of jobs into a map from board locators to .
 *
 * @param jobs
 *		The job information list.
 * @return
 *		Map from triad coords to job IDs, map from job ID to job information,
 *		and map from job ID to colour.
 */
function mapOfJobs(jobs: readonly MachineJobDescriptor[]) :
		[Map<string,number>, Map<number,MachineJobDescriptor>, Map<number,string>] {
	const m : Map<string,number> = new Map();
	const m2 : Map<number,MachineJobDescriptor> = new Map();
	const colours : Map<number,string> = new Map();

	/**
	 * Compute a hash of a number. Messy because we want "close" numbers to go
	 * to resulting values that are far apart.
	 *
	 * @param val
	 *		The number to compute a hash of.
	 * @return
	 *		The hash code, in the range [0.0, 1.0)
	 */
	function hashcode(val: number) : number {
		const str: string = `${ val }`.split("").reverse().join("");
		var hash = 0;
		if (str.length > 0) {
			for (var i = 0; i < str.length; i++) {
				// Code chosen to spread the bits low and high
				hash = hash * 0xA5459 + str.charCodeAt(i);
				hash = hash & hash; // Convert to 32bit integer
			}
		}
		return (hash % 10000) / 10000.0;
	};

	for (const j of jobs) {
		m2.set(j.id, j);
		const h = hashcode(j.id);
		colours.set(j.id, `hsl(${Math.floor(h * 360)}, 40%, 60%)`);
		for (const b of j.boards) {
			const {x:x, y:y, z:z} = b.triad;
			m.set(tuplekey([x,y,z]), j.id);
		}
	}
	return [m, m2, colours];
}

/**
 * Shared part of the tooltip control code.
 *
 * @param canv
 *		The main canvas element.
 * @param tooltip
 *		The tooltip canvas element.
 * @param tooltipCtx
 *		The drawing context for the tooltip.
 * @param locmapper
 *		How to look up coordinates.
 * @param scale
 *		General UI scaling factor.
 * @param triad
 *		Which triad (if any) are we setting the tooltip for.
 * @param message
 *		What message (if any) are we setting the tooltip to be. If omitted,
 *		we clear the tooltip. May be multiline.
 */
function setTooltipCore(
		canv: HTMLCanvasElement, tooltip: HTMLCanvasElement,
		tooltipCtx: CanvasRenderingContext2D, locmapper: LocMapper,
		scale: number, triad?: BoardTriad, message?: string) {
	if (triad === undefined || message === undefined) {
		tooltip.style.left = "-2000px";
		return;
	}
	const rect = canv.getBoundingClientRect();
	const hc = locmapper(triad);
	if (hc === undefined) {
		tooltip.style.left = "-2000px";
		return;
	}
	const [x, y] = hc[0];
	tooltip.style.top = (rect.top + y + scale + 10) + "px";
    tooltip.style.left = (rect.left + x - scale + 10) + "px";
    tooltipCtx.clearRect(0, 0, tooltip.width, tooltip.height);
    tooltipCtx.textAlign = "center";
	const tx = tooltip.getBoundingClientRect().width / 2;
	var ty = 15;
	for (const line of message.split("\n")) {
        tooltipCtx.fillText(line, tx, ty,
			tooltip.getBoundingClientRect().width - 5);
		const tm = tooltipCtx.measureText(line);
		ty += tm.actualBoundingBoxAscent + tm.actualBoundingBoxDescent;
	}
}

function setupCallbacks(
		canv: HTMLCanvasElement,
		getBoardInfo: GetBoardInfo,
		getJobInfo: GetJobInfo | undefined,
		tloc: Map<string, [BoardTriad, HexCoords]>,
		setCurrent: (newTriad: BoardTriad) => void,
		clearCurrent: (oldTriad: BoardTriad) => void) {
	var current : BoardTriad | undefined = undefined;
	function geturl() : string | undefined {
		if (current !== undefined) {
			const job = getJobInfo?.(current);
			if (job?.hasOwnProperty("url")) {
				return job.url;
			}
		}
		return undefined;
	}
	function configurePointer(asLink: boolean = false) {
		const c = canv.classList;
		if (asLink) {
			c.add("overlink");
		} else {
			c.remove("overlink");
		}
	}
	canv.addEventListener('mousemove', (e: MouseEvent) => {
		const triad = inside(e.offsetX, e.offsetY, tloc);
		if (current === triad) {
			return;
		}
		if (triad !== undefined) {
			if (current !== undefined) {
				clearCurrent(current);
			}
			setCurrent(triad);
			current = triad;
			configurePointer(geturl() !== undefined);
		} else if (current !== undefined) {
			clearCurrent(current);
			current = undefined;
			configurePointer();
		}
	});
	canv.addEventListener('mouseenter', (e: MouseEvent) => {
		const triad = inside(e.offsetX, e.offsetY, tloc);
		if (current === triad) {
			return;
		}
		if (triad !== undefined) {
			if (current !== undefined) {
				// I don't think this should be reachable
				clearCurrent(current);
			}
			setCurrent(triad);
			current = triad;
			configurePointer(geturl() !== undefined);
		} else if (current !== undefined) {
			clearCurrent(current);
			current = undefined;
			configurePointer();
		}
	});
	canv.addEventListener('mouseleave', (_: MouseEvent) => {
		if (current !== undefined) {
			clearCurrent(current);
			current = undefined;
			configurePointer();
		}
	});
	canv.addEventListener("click", (e: MouseEvent) => {
		if (current !== undefined) {
			const board = getBoardInfo(current);
			const job = getJobInfo?.(current);
			const url = job?.hasOwnProperty("url") ? job.url : undefined;
			if (url !== undefined) {
				// If we have a URL, go there now
				window.location.assign(url);
			} else {
				// Don't have anything; log for debugging purposes until we
				// decide what to do about it
				console.log("clicked", current, board, job, e);
			}
		}
	});
}

/**
 * Set up the size of the map canvas to something sensible.
 *
 * @param canvas
 *		The canvas to manipulate.
 * @param contentWidth
 *		The width of the content of the map, in triads.
 * @param contentHeight
 *		The height of the content of the map, in triads.
 * @return
 *		The coordinates of the root of the map and the scaling factors in
 *		the two dimensions.
 */
function initCanvasSize(
		canvas: HTMLCanvasElement,
		contentWidth: number, contentHeight: number):
		[number, number, number, number] {
	const m2 = mapMargin * 2;
	const basicCellWidth = contentWidth * 3 + 1;
	const basicCellHeight = contentHeight * 3 + 1;
	const minCanvWidth = basicCellWidth * minMapFactor + m2;
	const minCanvHeight = basicCellHeight * minMapFactor + m2;
	canvas.width = Math.max(canvas.width, minCanvWidth);
	canvas.height = Math.max(canvas.height, minCanvHeight);
	// TODO do we need to also manipulate the document CSS here?
	const rect = canvas.getBoundingClientRect();
	const rootX = mapMargin;
	const rootY = rect.height - mapMargin;
	const scaleX : number = (rect.width - m2) / basicCellWidth;
	const scaleY : number = (rect.height - m2) / basicCellHeight;
	return [rootX, rootY, scaleX, scaleY];
}

/**
 * Set up a canvas to illustrate a machine's boards.
 *
 * @param canvasId
 *		The name of the main canvas element.
 * @param tooltipId
 *		The name of the tooltip canvas element.
 * @param descriptor
 *		The information loaded about the machine.
 */
function drawMachine(
		canvasId: string, tooltipId: string,
		descriptor: MachineDescriptor) {
	// Get the canvases and drawing contexts
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const n_ctx = canv.getContext("2d");
	const tooltip = <HTMLCanvasElement> document.getElementById(tooltipId);
	const n_tooltipCtx = tooltip?.getContext("2d");
	if (n_ctx === null || n_tooltipCtx === null) {
		return;
	}
	const ctx = n_ctx, tooltipCtx = n_tooltipCtx;

	const [rootX, rootY, scaleX, scaleY] = initCanvasSize(
			canv, descriptor.width, descriptor.height);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	const live = boardMap(descriptor.live_boards);
	const dead = boardMap(descriptor.dead_boards);
	const [jobIdMap, jobMap, colourMap] = mapOfJobs(descriptor.jobs);

	ctx.strokeStyle = 'black';

	/** Where all the boards are on the canvas. */
	const tloc = drawLayout(ctx, rootX, rootY, scale,
			descriptor.width, descriptor.height, 3,
			(key: BoardTriad) => {
				// Colour selection function
				const k = tuplekey(key);
				if (dead.has(k)) {
					return "#444";
				}
				const j = jobIdMap.get(k);
				if (j !== undefined) {
					const c = colourMap.get(j);
					if (c !== undefined) {
						return c;
					}
				}
				if (live.has(k)) {
					return "white";
				} else {
					return "black";
				}
			}, (key : BoardTriad) => {
				// Label construction function
				const [x, y, z] = key;
				if (dead.has(tuplekey(key))) {
					return `\u2620 (${x},${y},${z})`;
				} else {
					return `(${x},${y},${z})`;
				}
			});

	/**
	 * Get the canvas coordinates of a board's hexagon. Wrapper around `tloc`.
	 *
	 * @param triad
	 *		The logical coordinates of the board.
	 * @return
	 *		The hex coordinates, or `undefined` if the board isn't known.
	 */
	function location(triad: BoardTriad) : HexCoords | undefined {
		return tloc.get(tuplekey(triad))?.[1];
	}

	/**
	 * Set or clear a tooltip.
	 *
	 * @param triad
	 *		The logical coordinates of the board.
	 *		Clear the tooltip if `undefined`.
	 * @param message
	 *		The tooltip message. Clear the tooltip if `undefined`.
	 */
	function setTooltip(triad?: BoardTriad, message?: string) {
		setTooltipCore(canv, tooltip, tooltipCtx, location, scale, triad, message);
	}

	/**
	 * Get the detailed description of a board.
	 *
	 * @param triad
	 *		Which board to describe.
	 * @return
	 *		The multiline description of the board.
	 */
	function triadDescription(triad: BoardTriad) : string {
		const [x, y, z] = triad;
		const key = tuplekey(triad);
		var s = `Triad: (X: ${x}, Y: ${y}, Z: ${z})`;
		const board = live.get(key) ?? dead.get(key);
		const id = jobIdMap.get(key);
		if (id !== undefined) {
			const job = jobMap.get(id);
			if (job !== undefined) {
				s += `\nJob ID: ${job.id}`;
				if (job.hasOwnProperty("owner")) {
					s += `\nOwner: ${job.owner}`;
				}
			}
		}
		if (board !== undefined) {
			s += `\nPhysical: [C: ${board.physical.cabinet}, F: ${board.physical.frame}, B: ${board.physical.board}]`;
			if (board.hasOwnProperty("network")) {
				const net = board.network;
				if (net != undefined) {
					s += "\nIP Address: " + net.address;
				}
			}
		} else {
			s += "\nBoard not present or\nnot managed by Spalloc."
		}
		return s;
	}

	/**
	 * Clear the current board/tooltip.
	 *
	 * @param triad
	 *		The old triad to clear.
	 */
	function clearCurrent(triad: BoardTriad) {
		ctx.strokeStyle = 'black';
		drawTriadBoard(ctx, rootX, rootY, scale, triad);
		setTooltip();
	}
	/**
	 * Set the current board/tooltip.
	 *
	 * @param triad
	 *		The new triad to set.
	 */
	function setCurrent(triad: BoardTriad) {
		ctx.strokeStyle = 'red';
		drawTriadBoard(ctx, rootX, rootY, scale, triad);
		setTooltip(triad, triadDescription(triad));
	}
	function getBoardInfo(triad: BoardTriad): BoardLocator | undefined {
		const key = tuplekey(triad);
		return live.get(key) ?? dead.get(key);
	}
	function getJobInfo(triad: BoardTriad): MachineJobDescriptor | undefined {
		const id = jobIdMap.get(tuplekey(triad));
		if (id !== undefined) {
			return jobMap.get(id);
		}
		return undefined;
	}
	setupCallbacks(canv, getBoardInfo, getJobInfo, tloc, setCurrent, clearCurrent);
};

/**
 * Set up a canvas to illustrate a job's allocation.
 * @param canvasId
 *		The name of the main canvas element.
 * @param tooltipId
 *		The name of the tooltip canvas element.
 * @param descriptor
 *		The information loaded about the job.
 */
function drawJob(
		canvasId: string, tooltipId: string,
		descriptor: JobDescriptor) {
	// Get the canvases and drawing contexts
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const n_ctx = canv.getContext("2d");
	const tooltip = <HTMLCanvasElement> document.getElementById(tooltipId);
	const n_tooltipCtx = tooltip?.getContext("2d");
	if (n_ctx === null || n_tooltipCtx === null) {
		return;
	}
	const ctx = n_ctx, tooltipCtx = n_tooltipCtx;

	const [rootX, rootY, scaleX, scaleY] = initCanvasSize(
			canv, descriptor.triad_width, descriptor.triad_height);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	/** Information about boards. Coordinates are machine-global. */
	const allocated = boardMap(descriptor.boards);

	ctx.strokeStyle = 'black';
	const {x: rx, y: ry} = descriptor.boards[0].triad;
	/** Where all the boards are on the canvas. Coordinates are job-local. */
	const tloc = drawLayout(ctx, rootX, rootY, scale,
			descriptor.triad_width, descriptor.triad_height, 3,
			triadCoord => {
				// Colour selection function
				const [x, y, z] = triadCoord;
				if (allocated.has(tuplekey([rx+x, ry+y, z]))) {
					return "yellow";
				}
				return "white";
			}, triadCoord => {
				// Label construction function
				const [x, y, z] = triadCoord;
				return `(${x},${y},${z})`;
			});
	/**
	 * Get the canvas coordinates of a board's hexagon. Wrapper around `tloc`.
	 *
	 * @param triad
	 *		The logical coordinates of the board.
	 * @return
	 *		The hex coordinates, or `undefined` if the board isn't known.
	 */
	function location(triad: BoardTriad) : HexCoords | undefined {
		return tloc.get(tuplekey(triad))?.[1];
	}

	/**
	 * Get the basic board location description. Wrapper around `allocated`.
	 *
	 * @param triad
	 *		Which board to describe.
	 * @return
	 *		The machine description of the board, or `undefined` if no board.
	 */
	function Board(triad: BoardTriad) : BoardLocator | undefined {
		const [x, y, z] = triad;
		return allocated.get(tuplekey([rx+x, ry+y, z]));
	}

	/**
	 * Set or clear a tooltip.
	 *
	 * @param triad
	 *		The logical coordinates of the board.
	 *		Clear the tooltip if `undefined`.
	 * @param message
	 *		The tooltip message. Clear the tooltip if `undefined`.
	 */
	function setTooltip(triad?: BoardTriad, message?: string) {
		setTooltipCore(canv, tooltip, tooltipCtx, location, scale, triad,
				message);
	}

	/**
	 * Get the detailed description of a board.
	 *
	 * @param triad
	 *		Which board to describe.
	 * @return
	 *		The multiline description of the board.
	 */
	function triadDescription(triad: BoardTriad) : string | undefined {
		const board = Board(triad);
		const [x, y, z] = triad;
		var s = `Board: (X: ${x}, Y: ${y}, Z: ${z})`;
		if (board?.hasOwnProperty("network")) {
			const net = board.network;
			if (net !== undefined) {
				s += `\nIP: ${net.address}`;
			}
		}
		return s;
	}

	/**
	 * Clear the current board/tooltip.
	 *
	 * @param triad
	 *		The old triad to clear.
	 */
	function clearCurrent(triad: BoardTriad) {
		ctx.strokeStyle = 'black';
		drawTriadBoard(ctx, rootX, rootY, scale, triad);
		setTooltip();
	}
	/**
	 * Set the current board/tooltip.
	 *
	 * @param triad
	 *		The new triad to set.
	 */
	function setCurrent(triad : BoardTriad) {
		ctx.strokeStyle = 'green';
		drawTriadBoard(ctx, rootX, rootY, scale, triad);
		setTooltip(triad, triadDescription(triad));
	}
	function getBoardInfo(triad: BoardTriad): BoardLocator | undefined {
		return allocated.get(tuplekey(triad));
	}
	setupCallbacks(canv, getBoardInfo, undefined, tloc, setCurrent, clearCurrent);
}

/**
 * Convert a JSON document into a form more digestible for people.
 *
 * @param elementId
 *		The ID of the element JSON document to pretty-print.
 */
function prettyJson(elementId: string) {
	const element = document.getElementById(elementId);
	const content = element?.textContent;
	if (content === null || content == undefined) {
		return;
	}
	const pretty = JSON.stringify(JSON.parse(content), null, 2);
	element!.textContent = pretty;
}

/**
 * Convert a timestamp into a form more digestible for people.
 *
 * @param elementId
 *		The ID of the element containing the timestamp.
 */
function prettyTimestamp(elementId: string) {
	const element = document.getElementById(elementId);
	const content = element?.textContent;
	if (content === null || content === undefined) {
		return;
	}
	const timestamp = new Date(content);
	const dtf = new Intl.DateTimeFormat([], {
		year: 'numeric',
		month: 'long',
		day: 'numeric',
		hour: 'numeric',
		minute: 'numeric'
	});
	element!.textContent = dtf.format(timestamp);
}

/**
 * Convert a timestamp into a form more digestible for people.
 *
 * @param elementId
 *		The ID of the element containing the timestamp.
 */
function prettyDuration(elementId: string) {
	const element = document.getElementById(elementId);
	var content = element?.textContent;
	if (content === null || content === undefined) {
		return;
	}
	content = content.replace(/^PT/, "").replace("H", " hours ").replace(
		"M", " minutes ").replace("S", " seconds ");
	content = content.replace(new RegExp(/\b1 (\w+)s/, "g"), "1 $1");
	content = content.replace(new RegExp(/\b( \d)/, "g"), ",$1");
	element!.textContent = content;
}

/**
 * Load temperature data from a URL.
 *
 * @param sourceUri
 * 		The URL to load the data from.
 * @param boardId
 * 		Which board this is about.
 * @param elementId
 * 		Which element to replace the contents of with with the rendered result.
 */
// TODO Do we need an explicit boardId? Is the value in the URI already?
function loadTemperature(sourceUri: string, boardId: number, elementId: string) {
	const element = document.getElementById(elementId);
	if (element == null) {
		return;
	}
	const r = new XMLHttpRequest();
	r.open("GET", sourceUri);
	r.onload = () => {
		const result = JSON.parse(r.response) as object;
		if (result?.hasOwnProperty("result.board_temperature")) {
			const t = result["board_temperature"] as number;
			element.innerHTML = t + "&deg;C";
		}
	};
	r.send();
}
