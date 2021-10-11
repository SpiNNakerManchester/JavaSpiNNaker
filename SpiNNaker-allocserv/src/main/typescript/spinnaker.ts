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
 * Draw a single board cell. This is a distorted hexagon.
 *
 * @param {CanvasRenderingContext2D} ctx
 *		The drawing context.
 * @param {number} x
 *		The canvas X coordinate.
 * @param {number} y
 *		The canvas Y coordinate.
 * @param {number} scale
 *		The fundamental length scale for the size of boards.
 * @param {boolean} fill
 *		Whether to fill the cell or just draw the outline.
 * @param {string} label
 *		What label to put in the cell, if any.
 * @returns {HexCoords}
 *		The coordinates of the vertices of the cell.
 */
function drawBoard(
		ctx : CanvasRenderingContext2D,
		x : number, y : number, scale : number,
		fill : boolean = false,
		label : string = undefined) : HexCoords {
	ctx.beginPath();
	var coords : [number, number][] = [];
	coords.push([x, y]);
	ctx.moveTo(x, y);
	coords.push([x + scale, y]);
	ctx.lineTo(x + scale, y);
	coords.push([x + 2 * scale, y - scale]);
	ctx.lineTo(x + 2 * scale, y - scale);
	coords.push([x + 2 * scale, y - 2 * scale]);
	ctx.lineTo(x + 2 * scale, y - 2 * scale);
	coords.push([x + scale, y - 2 * scale]);
	ctx.lineTo(x + scale, y - 2 * scale);
	coords.push([x, y - scale]);
	ctx.lineTo(x, y - scale);
	ctx.closePath();
	if (fill) {
		ctx.fill();
	}
	ctx.stroke();
	if (label !== undefined) {
		ctx.save();
		ctx.fillStyle = ctx.strokeStyle;
		ctx.textAlign = "center";
		ctx.fillText(label, x + scale, y - scale, 2 * scale);
		ctx.restore();
	}
	return coords;
}

/**
 * Draw a single board cell, identified in machine coordinates. This is a
 * distorted hexagon.
 *
 * @param {CanvasRenderingContext2D} ctx
 *		The drawing context.
 * @param {number} rootX
 *		The canvas X coordinate of the bottom left of the machine.
 * @param {number} rootY
 *		The canvas Y coordinate of the bottom left of the machine.
 * @param {number} scale
 *		The fundamental length scale for the size of boards.
 * @param {BoardTriad} triadCoords
 *		Triad coordinates of the board to draw
 * @param {boolean} fill
 *		Whether to fill the cell or just draw the outline.
 * @param {(BoardTriad) => string} labeller
 *		Function to generate the label.
 * @returns {HexCoords}
 *		The coordinates of the vertices of the cell.
 */
function drawTriadBoard(
		ctx : CanvasRenderingContext2D,
		rootX : number, rootY : number, scale : number,
		triadCoords : BoardTriad,
		fill : boolean = false,
		labeller : (key: BoardTriad) => string = undefined) : HexCoords {
	const [x, y, z] = triadCoords;
	var bx : number = rootX + x * 3 * scale;
	var by : number = rootY - y * 3 * scale;
	if (z == 1) {
		bx += 2 * scale;
		by -= scale;
	} else if (z == 2) {
		bx += scale;
		by -= 2 * scale;
	}
	var label : string = undefined;
	if (labeller !== undefined) {
		label = labeller(triadCoords);
	}
	return drawBoard(ctx, bx, by, scale, fill, label);
}

/**
 * Convert board triad coordinates into a key suitable for a Map.
 *
 * @param {BoardCoords} coords
 *		The coordinates.
 * @return {string}
 * 		The key string
 */
function tuplekey(coords: BoardTriad) : string {
	const [x, y, z] = coords;
	return x + "," + y + "," + z;
}

/**
 * Convert a list of board locators into a map using the triad coords as key.
 *
 * @param {CanvasRenderingContext2D} ctx
 *		How to draw on the canvas.
 * @param {number} rootX
 *		The root X coordinate for drawing.
 * @param {number} rootY
 *		The root Y coordinate for drawing.
 * @param {number} scale
 *		The diagram basic scaling factor.
 * @param {number} width
 *		The width of the diagram, in triads.
 * @param {number} height
 *		The height of the diagram, in triads.
 * @param {number} depth
 *		The depth of the diagram (usually 1 or 3).
 * @param {string | ((key: BoardTriad) => string)} colourer
 *		How to get a colour for a board.
 * @param {(key: BoardTriad) => string} labeller
 *		How to get the label for a board.
 * @return {Map<string,[BoardTriad,HexCoords]>}
 *		Description of where each board was drawn.
 */
function drawLayout(
		ctx : CanvasRenderingContext2D,
		rootX : number, rootY : number, scale : number,
		width : number, height : number, depth : number = 3,
		colourer : string | ((key: BoardTriad) => string) = undefined,
		labeller : (key: BoardTriad) => string = undefined) : Map<string,[BoardTriad,HexCoords]> {
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
 * @param {number} x
 *		Canvas X coordinate
 * @param {number} y
 *		Canvas Y coordinate
 * @param {Map<string,[BoardTriad,HexCoords]>} tloc
 *		Location mapping from `drawLayout()`
 * @return {BoardTriad}
 *		Which board the point is in, or `undefined` if none.
 */
// https://stackoverflow.com/a/29915728/301832
function inside(
		x : number, y : number,
		tloc : Map<string,[BoardTriad,HexCoords]>) : BoardTriad {
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
 * @param {BoardLocator[]} boards
 *		The name of the main canvas element.
 * @return {Map<string,BoardLocator>}
 *		Map from triad coords to full locators,
 */
function boardMap(boards: readonly BoardLocator[]) : Map<string,BoardLocator> {
	var m : Map<string,BoardLocator> = new Map();
	for (const b of boards) {
		const {x:x, y:y, z:z} = b.triad;
		m.set(tuplekey([x,y,z]), b);
	}
	return m;
}

/**
 * Shared part of the tooltip control code.
 *
 * @param {HTMLCanvasElement} canv
 *		The main canvas element.
 * @param {HTMLCanvasElement} tooltip
 *		The tooltip canvas element.
 * @param {CanvasRenderingContext2D} tooltipCtx
 *		The drawing context for the tooltip.
 * @param {((t: BoardTriad) => HexCoords)} locmapper
 *		How to look up coordinates.
 * @param {number} scale
 *		General UI scaling factor.
 * @param {BoardTriad?} triad
 *		Which triad (if any) are we setting the tooltip for.
 * @param {string?} message
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
		tooltip.style.top = (rect.top + x + scale + 10) + "px";
        tooltip.style.left = (rect.left + y - scale + 10) + "px";
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

/**
 * Set up a canvas to illustrate a machine's boards.
 *
 * @param {string} canvasId
 *		The name of the main canvas element.
 * @param {string} tooltipId
 *		The name of the tooltip canvas element.
 * @param {MachineDescriptor} descriptor
 *		The information loaded about the machine.
 */
function drawMachine(
		canvasId : string, tooltipId : string,
		descriptor : MachineDescriptor) {
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const rect = canv.getBoundingClientRect();
	const tooltip = <HTMLCanvasElement> document.getElementById(tooltipId);
	const rootX = 5;
	const rootY = rect.height - 5;
	const scaleX : number = (rect.width - 10) / (descriptor.width * 3 + 1);
	const scaleY : number = (rect.height - 10) / (descriptor.height * 3 + 1);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	const ctx = canv.getContext("2d");
	const tooltipCtx = tooltip.getContext("2d");

	const live = boardMap(descriptor.live_boards);
	const dead = boardMap(descriptor.dead_boards);

	ctx.strokeStyle = 'black';

	/** Where all the boards are on the canvas. */
	const tloc = drawLayout(ctx, rootX, rootY, scale,
			descriptor.width, descriptor.height, 3,
			(key: BoardTriad) => {
				// Colour selection function
				const k = tuplekey(key);
				if (dead.has(k)) {
					return "#444";
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
	 * @param {BoardTriad} triad
	 *		The logical coordinates of the board.
	 * @return {HexCoords}
	 *		The hex coordinates, or `undefined` if the board isn't known.
	 */
	function location(triad: BoardTriad) : HexCoords {
		const t = tloc.get(tuplekey(triad));
		return t == undefined ? undefined : t[1];
	}

	/**
	 * Set or clear a tooltip.
	 *
	 * @param {BoardTriad?} triad
	 *		The logical coordinates of the board.
	 *		Clear the tooltip if `undefined`.
	 * @param {string?} message
	 *		The tooltip message. Clear the tooltip if `undefined`.
	 */
	function setTooltip(triad?: BoardTriad, message?: string) {
		setTooltipCore(canv, tooltip, tooltipCtx, location, scale, triad, message);
	}

	/**
	 * Get the detailed description of a board.
	 *
	 * @param {BoardTriad} triad
	 *		Which board to describe.
	 * @return {string}
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
		if (board !== undefined) {
			s += `\nPhysical: [C: ${board.physical.cabinet}, F: ${board.physical.frame}, B: ${board.physical.board}]`;
			if (board.network !== undefined) {
				s += "\nIP Address: " + board.network.address;
			}
		} else {
			s += "\nBoard not present or\nnot managed by Spalloc."
		}
		return s;
	}

	/** The current board (i.e., that has the mouse over it). */
	var current : BoardTriad = undefined;
	/** Common code: clear the current board/tooltip. */
	function clearCurrent() {
		ctx.strokeStyle = 'black';
		drawTriadBoard(ctx, rootX, rootY, scale, current);
		setTooltip();
		current = undefined;
	}
	/** Common code: set the current board/tooltip. */
	function setCurrent(triad : BoardTriad) {
		ctx.strokeStyle = 'red';
		drawTriadBoard(ctx, rootX, rootY, scale, triad);
		setTooltip(triad, triadDescription(triad));
		current = triad;
	}

	canv.addEventListener('mousemove', (e: MouseEvent) => {
		const triad = inside(e.offsetX, e.offsetY, tloc);
		if (current === triad) {
			return;
		}
		if (triad !== undefined) {
			if (current !== undefined) {
				clearCurrent();
			}
			setCurrent(triad);
		} else if (current !== undefined) {
			clearCurrent();
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
				clearCurrent();
			}
			setCurrent(triad);
		} else if (current !== undefined) {
			clearCurrent();
		}
	});
	canv.addEventListener('mouseleave', (_: MouseEvent) => {
		if (current !== undefined) {
			clearCurrent();
		}
	});
};

/**
 * Set up a canvas to illustrate a job's allocation.
 * @param {string} canvasId
 *		The name of the main canvas element.
 * @param {string} tooltipId
 *		The name of the tooltip canvas element.
 * @param {JobDescriptor} descriptor
 *		The information loaded about the job.
 */
function drawJob(
		canvasId : string, tooltipId : string,
		descriptor : JobDescriptor) {
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const rect = canv.getBoundingClientRect();
	const tooltip = <HTMLCanvasElement> document.getElementById(tooltipId);
	const rootX = 5;
	const rootY = rect.height - 5;
	const scaleX : number = (rect.width - 10) / (descriptor.triad_width * 3 + 1);
	const scaleY : number = (rect.height - 10) / (descriptor.triad_height * 3 + 1);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	const ctx = canv.getContext("2d");
	const tooltipCtx = tooltip.getContext("2d");

	const allocated = boardMap(descriptor.boards);

	ctx.strokeStyle = 'black';
	const {x: rx, y: ry} = descriptor.boards[0].triad;
	/** Where all the boards are on the canvas. */
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
	 * @param {BoardTriad} triad
	 *		The logical coordinates of the board.
	 * @return {HexCoords}
	 *		The hex coordinates, or `undefined` if the board isn't known.
	 */
	function location(triad: BoardTriad) : HexCoords {
		const t = tloc.get(tuplekey(triad));
		return t == undefined ? undefined : t[1];
	}

	/**
	 * Set or clear a tooltip.
	 *
	 * @param {BoardTriad?} triad
	 *		The logical coordinates of the board.
	 *		Clear the tooltip if `undefined`.
	 * @param {string?} message
	 *		The tooltip message. Clear the tooltip if `undefined`.
	 */
	function setTooltip(triad?: BoardTriad, message?: string) {
		setTooltipCore(canv, tooltip, tooltipCtx, location, scale, triad,
				message);
	}

	/**
	 * Get the detailed description of a board.
	 *
	 * @param {BoardTriad} triad
	 *		Which board to describe.
	 * @return {string | undefined}
	 *		The multiline description of the board.
	 */
	function triadDescription(triad: BoardTriad) : string | undefined {
		const [x, y, z] = triad;
		var board : BoardLocator = undefined;
		if (allocated.has(tuplekey(triad))) {
			board = allocated.get(tuplekey(triad));
		}
		if (board !== null) {
			var s = `Board: (X: ${x}, Y: ${y}, Z: ${z})`;
			if (board.network !== undefined) {
				s += `\nIP: ${board.network.address}`;
			}
			return s;
		}
		return undefined;
	}

	/** The current board (i.e., that has the mouse over it). */
	var current : BoardTriad = undefined;
	/** Common code: clear the current board/tooltip. */
	function clearCurrent() {
		ctx.strokeStyle = 'black';
		drawTriadBoard(ctx, rootX, rootY, scale, current);
		setTooltip();
		current = undefined;
	}
	/** Common code: set the current board/tooltip. */
	function setCurrent(triad : BoardTriad) {
		ctx.strokeStyle = 'green';
		drawTriadBoard(ctx, rootX, rootY, scale, triad);
		setTooltip(triad, triadDescription(triad));
		current = triad;
	}

	canv.addEventListener('mousemove', (e: MouseEvent) => {
		const triad = inside(e.offsetX, e.offsetY, tloc);
		if (current === triad) {
			return;
		}
		if (triad !== undefined) {
			if (current !== undefined) {
				clearCurrent();
			}
			setCurrent(triad);
		} else if (current !== undefined) {
			clearCurrent();
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
				clearCurrent();
			}
			setCurrent(triad);
		} else if (current !== undefined) {
			clearCurrent();
		}
	});
	canv.addEventListener('mouseleave', (_: MouseEvent) => {
		if (current !== undefined) {
			clearCurrent();
		}
	});
}
