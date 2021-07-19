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

function drawBoard(
		ctx : CanvasRenderingContext2D,
		x : number, y : number, scale : number,
		fill : boolean = false) : number[][] {
	ctx.beginPath();
	var coords : number[][] = [];
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
	return coords;
};

function drawTriadBoard(
		ctx : CanvasRenderingContext2D,
		rootX : number, rootY : number, scale : number,
		x : number, y : number, z : number,
		fill : boolean = false) : number[][] {
	var bx : number = rootX + x * 3 * scale;
	var by : number = rootY - y * 3 * scale;
	if (z == 1) {
		bx += 2 * scale;
		by -= scale;
	} else if (z == 2) {
		bx += scale;
		by -= 2 * scale;
	}
	return drawBoard(ctx, bx, by, scale, fill);
};

function drawLayout(
		ctx : CanvasRenderingContext2D,
		rootX : number, rootY : number, scale : number,
		width : number, height : number, depth : number,
		fill : string = "white") : Map<number[],number[][]> {
	var tloc : Map<number[],number[][]> = new Map();
	for (var y : number = 0; y < height; y++) {
		for (var x : number = 0; x < width; x++) {
			for (var z : number = 0; z <= depth; z++) {
				ctx.fillStyle = fill;
				tloc.set([x,y,z], drawTriadBoard(
						ctx, rootX, rootY, scale, x, y, z, true));
			}
		}
	}
	return tloc;
};

// https://stackoverflow.com/a/29915728/301832
function inside(
		x : number, y : number,
		tloc : Map<number[],number[][]>) : number[] {
    // ray-casting algorithm based on
    // https://wrf.ecse.rpi.edu/Research/Short_Notes/pnpoly.html/pnpoly.html

	for (const key of tloc) {
		var triad = key[0], poly = key[1];
	    var inside = false;
	    for (var i = 0, j = poly.length - 1; i < poly.length; j = i++) {
	        var xi = poly[i][0], yi = poly[i][1];
	        var xj = poly[j][0], yj = poly[j][1];

	        var intersect = ((yi > y) != (yj > y))
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
		rootX : number, rootY : number, scale : number,
		descriptor) {
	var canv = <HTMLCanvasElement> document.getElementById(canvasId);
	var ctx : CanvasRenderingContext2D = canv.getContext("2d");
	ctx.strokeStyle = 'black';
	var tloc = drawLayout(ctx, rootX, rootY, scale,
			descriptor.width, descriptor.height, 3);
	var current : number[] = undefined;
	function motion(e : MouseEvent) {
		const x = e.offsetX, y = e.offsetY;
		var triad = inside(x, y, tloc);
		if (triad !== undefined) {
			if (current !== undefined) {
				if (current === triad) {
					return;
				}
				ctx.strokeStyle = 'black';
				drawTriadBoard(ctx, rootX, rootY, scale,
						current[0], current[1], current[2]);
			}
			ctx.strokeStyle = 'red';
			drawTriadBoard(ctx, rootX, rootY, scale,
					triad[0], triad[1], triad[2])
		} else if (current !== undefined) {
			ctx.strokeStyle = 'black';
			drawTriadBoard(ctx, rootX, rootY, scale,
					current[0], current[1], current[2]);
		}
		current = triad;
	}
	canv.addEventListener('mousemove', motion);
};
