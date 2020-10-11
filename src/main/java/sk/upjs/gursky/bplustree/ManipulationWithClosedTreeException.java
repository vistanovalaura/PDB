/*
 * sk.upjs.gursky.bplustree.ManipulationWithClosedTreeException.java	ver 1.0, February 5th 2009
 *
 *	   Copyright 2009 Peter Gursky. All rights reserved.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *     
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *     
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.upjs.gursky.bplustree;

public class ManipulationWithClosedTreeException extends RuntimeException {

	private static final long serialVersionUID = 4120196159831672398L;

	ManipulationWithClosedTreeException() {
		super();
	}

	ManipulationWithClosedTreeException(String message, Throwable cause) {
		super(message, cause);
	}

	ManipulationWithClosedTreeException(String message) {
		super(message);
	}

	ManipulationWithClosedTreeException(Throwable cause) {
		super(cause);
	}
}
