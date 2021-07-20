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
	// TODO list dead boards too
};

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
};

function drawTriadBoard(
		ctx : CanvasRenderingContext2D,
		rootX : number, rootY : number, scale : number,
		key : BoardTriad,
		fill : boolean = false,
		labeller : (key: BoardTriad) => string = undefined) : HexCoords {
	const [x, y, z] = key;
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
		label = labeller(key);
	}
	return drawBoard(ctx, bx, by, scale, fill, label);
};

function drawLayout(
		ctx : CanvasRenderingContext2D,
		rootX : number, rootY : number, scale : number,
		width : number, height : number, depth : number = 3,
		colourer : string | ((key: BoardTriad) => string) = undefined,
		labeller : (key: BoardTriad) => string = undefined) : Map<BoardTriad,HexCoords> {
	var tloc : Map<BoardTriad,HexCoords> = new Map();
	for (var y : number = 0; y < height; y++) {
		for (var x : number = 0; x < width; x++) {
			for (var z : number = 0; z < depth; z++) {
				const key = [x, y, z] as const;
				if (typeof colourer === 'string') {
					ctx.fillStyle = colourer;
				} else {
					ctx.fillStyle = colourer(key);
				}
				tloc.set(key, drawTriadBoard(
						ctx, rootX, rootY, scale, key, true, labeller));
			}
		}
	}
	return tloc;
};

// https://stackoverflow.com/a/29915728/301832
function inside(
		x : number, y : number,
		tloc : Map<BoardTriad,HexCoords>) : BoardTriad {
    // ray-casting algorithm based on
    // https://wrf.ecse.rpi.edu/Research/Short_Notes/pnpoly.html/pnpoly.html

	for (const item of tloc) {
		const [triad, poly] = item;
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
};

function initialDrawAllocation(
		canvasId : string,
		descriptor : MachineDescriptor) {
	const canv = <HTMLCanvasElement> document.getElementById(canvasId);
	const rootX = 5;
	const rootY = canv.height - 5;
	const scaleX : number = (canv.width - 10) / (descriptor.width * 3 + 1);
	const scaleY : number = (canv.height - 10) / (descriptor.height * 3 + 1);
	const scale = (scaleX < scaleY) ? scaleX : scaleY;
	const ctx : CanvasRenderingContext2D = canv.getContext("2d");

	ctx.strokeStyle = 'black';
	function lbl(key : BoardTriad) {
		const [x, y, z] = key;
		return "(" + x + "," + y + "," + z + ")";
	}
	const tloc = drawLayout(ctx, rootX, rootY, scale,
			descriptor.width, descriptor.height, 3,
			"white", lbl);
	var current : BoardTriad = undefined;
	function motion(e : MouseEvent) {
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
		} else if (current !== undefined) {
			ctx.strokeStyle = 'black';
			drawTriadBoard(ctx, rootX, rootY, scale, current);
		}
		current = triad;
	}
	canv.addEventListener('mousemove', motion);
};
