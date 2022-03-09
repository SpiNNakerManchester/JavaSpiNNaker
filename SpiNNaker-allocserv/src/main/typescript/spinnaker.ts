// Copyright (c) 2021 The University of Manchester
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

/** The minimum size of a basic cell of the map. Hexes are made of several of these. */
const minMapFactor = 12;
/** The number of pixels to allow for the margin on maps. */
const mapMargin = 5;

/** Machine logical global coordinate space (x, y, z). */
type BoardTriad = readonly [number, number, number];
/** The list of canvas coordinates of a hexagon representing a board. */
type HexCoords = readonly [number, number][];

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
		labeller: (key: BoardTriad) => string = undefined) : HexCoords {
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
	var label : string = undefined;
	if (labeller !== undefined) {
		label = labeller(triadCoords);
	}

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
		colourer: string | ((key: BoardTriad) => string) = undefined,
		labeller: (key: BoardTriad) => string = undefined) :
			Map<string,[BoardTriad,HexCoords]> {
	var tloc : Map<string,[BoardTriad,HexCoords]> = new Map();
	for (var y : number = 0; y < height; y++) {
		for (var x : number = 0; x < width; x++) {
			for (var z : number = 0; z < depth; z++) {
				const key = [x, y, z] as const;
				if (typeof colourer === 'string') {
					ctx.fillStyle = colourer;
				} else {
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
		tloc: Map<string,[BoardTriad,HexCoords]>) : BoardTriad {
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
		tooltipCtx: CanvasRenderingContext2D,
		locmapper: ((t: BoardTriad) => HexCoords),
		scale: number, triad?: BoardTriad, message?: string) {
	if (triad === undefined || message === undefined) {
		tooltip.style.left = "-2000px";
	} else {
		const rect = canv.getBoundingClientRect();
		const [x, y] = locmapper(triad)[0];
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
}

function setupCallbacks(
		canv: HTMLCanvasElement,
		getBoardInfo: ((board: BoardTriad) => BoardLocator),
		getJobInfo: ((board: BoardTriad) => MachineJobDescriptor) | undefined,
		tloc: Map<string, [BoardTriad, HexCoords]>,
		setCurrent: ((newTriad: BoardTriad) => void),
		clearCurrent: ((oldTriad: BoardTriad) => void)) {
	var current : BoardTriad = undefined;
	function geturl() : string {
		if (getJobInfo !== undefined) {
			const job = getJobInfo(current);
			if (job !== undefined && job.hasOwnProperty("url")) {
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
			var job: MachineJobDescriptor = undefined;
			var url = undefined;
			if (getJobInfo !== undefined) {
				job = getJobInfo(current);
				if (job !== undefined && job.hasOwnProperty("url")) {
					url = job.url;
				}
			}
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
	const minCanv§Width = basicCellWidth * minMapFactor + m2;
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
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const [rootX, rootY, scaleX, scaleY] = initCanvasSize(
			canv, descriptor.width, descriptor.height);
	const tooltip = <HTMLCanvasElement> document.getElementById(tooltipId);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	const ctx = canv.getContext("2d");
	const tooltipCtx = tooltip.getContext("2d");

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
				} else if (jobIdMap.has(k)) {
					return colourMap.get(jobIdMap.get(k));
				} else if (live.has(k)) {
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
	function location(triad: BoardTriad) : HexCoords {
		const t = tloc.get(tuplekey(triad));
		return t == undefined ? undefined : t[1];
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
		var board : BoardLocator = undefined;
		if (live.has(key)) {
			board = live.get(key);
		} else if (dead.has(key)) {
			board = dead.get(key);
		}
		if (jobIdMap.has(key)) {
			const job = jobMap.get(jobIdMap.get(key));
			s += `\nJob ID: ${job.id}`;
			if (job.hasOwnProperty("owner")) {
				s += `\nOwner: ${job.owner}`;
			}
		}
		if (board !== undefined) {
			s += `\nPhysical: [C: ${board.physical.cabinet}, F: ${board.physical.frame}, B: ${board.physical.board}]`;
			if (board.hasOwnProperty("network")) {
				s += "\nIP Address: " + board.network.address;
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
	function getBoardInfo(triad: BoardTriad): BoardLocator {
		const key = tuplekey(triad);
		if (live.has(key)) {
			return live.get(key);
		}
		if (dead.has(key)) {
			return dead.get(key);
		}
		return undefined;
	}
	function getJobInfo(triad: BoardTriad): MachineJobDescriptor {
		const key = tuplekey(triad);
		if (jobIdMap.has(key)) {
			const id = jobIdMap.get(key);
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
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const [rootX, rootY, scaleX, scaleY] = initCanvasSize(
			canv, descriptor.triad_width, descriptor.triad_height);
	const tooltip = <HTMLCanvasElement> document.getElementById(tooltipId);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	const ctx = canv.getContext("2d");
	const tooltipCtx = tooltip.getContext("2d");

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
	function location(triad: BoardTriad) : HexCoords {
		const t = tloc.get(tuplekey(triad));
		return t == undefined ? undefined : t[1];
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
		const key = tuplekey([rx+x, ry+y, z]);
		var board : BoardLocator = undefined;
		if (allocated.has(key)) {
			board = allocated.get(key);
		}
		return board
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
		if (board !== undefined && board.hasOwnProperty("network")) {
			s += `\nIP: ${board.network.address}`;
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
	function getBoardInfo(triad: BoardTriad): BoardLocator {
		const key = tuplekey(triad);
		if (allocated.has(key)) {
			return allocated.get(key);
		}
		return undefined;
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
	const pretty = JSON.stringify(JSON.parse(element.textContent), null, 2);
	element.textContent = pretty;
}

/**
 * Convert a timestamp into a form more digestible for people.
 *
 * @param elementId
 *		The ID of the element containing the timestamp.
 */
function prettyTimestamp(elementId: string) {
	const element = document.getElementById(elementId);
	const timestamp = new Date(element.textContent);
	const dtf = new Intl.DateTimeFormat([], {
		year: 'numeric',
		month: 'long',
		day: 'numeric',
		hour: 'numeric',
		minute: 'numeric'
	});
	const pretty = dtf.format(timestamp);
	element.textContent = pretty;
}

/**
 * Convert a timestamp into a form more digestible for people.
 *
 * @param elementId
 *		The ID of the element containing the timestamp.
 */
function prettyDuration(elementId: string) {
	const element = document.getElementById(elementId);
	var content: string = element.textContent;
	content = content.replace(/^PT/, "").replace("H", " hours ").replace(
		"M", " minutes ").replace("S", " seconds ");
	content = content.replace(new RegExp(/\b1 (\w+)s/, "g"), "1 $1");
	content = content.replace(new RegExp(/\b( \d)/, "g"), ",$1");
	element.textContent = content;
}
