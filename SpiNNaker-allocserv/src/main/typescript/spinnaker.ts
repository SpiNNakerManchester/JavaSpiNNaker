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

type BoardTriad = readonly [number, number, number];
type HexCoords = readonly [number, number][];

interface BoardLocator {
	triad: {
		x: number;
		y: number;
		z: number;
	};
	physical: {
		cabinet: number;
		frame: number;
		board: number;
	};
	network: {
		address: string;
	};
};

interface MachineJobDescriptor {
	id: number;
	url?: string;
	owner?: string;
	boards: BoardLocator[];
};

interface MachineDescriptor {
	name: string;
	width: number;
	height: number;
	num_in_use: number;
	tags: string[];
	jobs: MachineJobDescriptor[];
	live_boards: BoardLocator[];
	dead_boards: BoardLocator[];
};

interface JobDescriptor {
	id: number;
	owner?: string;
	state: string;
	start: string; // Instant
	keep_alive: string; // Duration
	owner_host?: string;
	width?: number;
	height?: number;
	powered: boolean;
	machine: string;
	machine_url: string;
	boards: BoardLocator[];
	request?: any;
	triad_width: number;
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
 * @param {BoardCoords} coords
 *		The coordinates.
 * @return {string}
 * 		The key string
 */
function tuplekey(coords: BoardTriad) : string {
	const [x, y, z] = coords;
	return x + "," + y + "," + z;
}

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

function boardMap(boards: readonly BoardLocator[]) : Map<string,BoardLocator> {
	var m : Map<string,BoardLocator> = new Map();
	for (const b of boards) {
		const {x:x, y:y, z:z} = b.triad;
		m.set(tuplekey([x,y,z]), b);
	}
	return m;
}

function setTooltipCore(
		canv: HTMLCanvasElement, tooltip: HTMLCanvasElement,
		tooltipCtx: CanvasRenderingContext2D,
		locmapper: ((t: BoardTriad) => HexCoords),
		scale:number, triad?: BoardTriad, message?: string) {
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

	function lbl(key : BoardTriad) : string {
		const [x, y, z] = key;
		if (dead.has(tuplekey(key))) {
			return `\u2620 (${x},${y},${z})`;
		} else {
			return `(${x},${y},${z})`;
		}
	}

	function clr(key: BoardTriad) : string {
		const k = tuplekey(key);
		if (dead.has(k)) {
			return "#444";
		} else if (live.has(k)) {
			return "white";
		} else {
			return "black";
		}
	}

	const tloc = drawLayout(ctx, rootX, rootY, scale,
			descriptor.width, descriptor.height, 3,
			clr, lbl);
	function location(triad: BoardTriad) : HexCoords {
		const t = tloc.get(tuplekey(triad));
		return t == undefined ? undefined : t[1];
	}

	function setTooltip(triad?: BoardTriad, message?: string) {
		setTooltipCore(canv, tooltip, tooltipCtx, location, scale, triad, message);
	}

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

	var current : BoardTriad = undefined;
	function motion(e: MouseEvent) {
		const x = e.offsetX, y = e.offsetY;
		const triad = inside(x, y, tloc);
		if (triad !== undefined) {
			if (current !== undefined) {
				if (current === triad) {
					return;
				}
				ctx.strokeStyle = 'black';
				drawTriadBoard(ctx, rootX, rootY, scale, current);
			}
			ctx.strokeStyle = 'red';
			drawTriadBoard(ctx, rootX, rootY, scale, triad);
			setTooltip(triad, triadDescription(triad));
		} else if (current !== undefined) {
			ctx.strokeStyle = 'black';
			drawTriadBoard(ctx, rootX, rootY, scale, current);
			setTooltip();
		}
		current = triad;
	}
	function enter(e: MouseEvent) {
		const x = e.offsetX, y = e.offsetY;
		const triad = inside(x, y, tloc);
		if (triad !== undefined) {
			if (current !== undefined) {
				/* Should be unreachable... */
				if (current === triad) {
					return;
				}
				ctx.strokeStyle = 'black';
				drawTriadBoard(ctx, rootX, rootY, scale, current);
			}
			ctx.strokeStyle = 'red';
			drawTriadBoard(ctx, rootX, rootY, scale, triad);
			setTooltip(triad, triadDescription(triad));
		}
	}
	function leave(e: MouseEvent) {
		e.offsetX;
		if (current !== undefined) {
			ctx.strokeStyle = 'black';
			drawTriadBoard(ctx, rootX, rootY, scale, current);
			setTooltip();
		}
	}

	canv.addEventListener('mousemove', motion);
	canv.addEventListener('mouseenter', enter);
	canv.addEventListener('mouseleave', leave);
};

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
	//FIXME define tooltips and bindings for the job view
	const tooltipCtx = tooltip.getContext("2d");

	const allocated = boardMap(descriptor.boards);

	ctx.strokeStyle = 'black';
	const {x: rx, y: ry} = descriptor.boards[0].triad;
	drawLayout(ctx, rootX, rootY, scale,
			descriptor.triad_width, descriptor.triad_height, 3,
			triadCoord => {
				const [x, y, z] = triadCoord;
				if (allocated.has(tuplekey([rx+x, ry+y, z]))) {
					return "yellow";
				}
				return "white";
			}, triadCoord => {
				const [x, y, z] = triadCoord;
				return `(${x},${y},${z})`;
			});
}
