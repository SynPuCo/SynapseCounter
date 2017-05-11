/*

 Written by Andrey Rozenberg (jaera at yandex.com)
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program. If not, see <http://www.gnu.org/licenses/>.

*/

import Utilities.Counter3D;
import Utilities.Object3D;
import java.util.Vector;
import ij.ImagePlus;

public class MyParticleAnalyzer3D {
	private int myCount = 0;          // total number of particles
	private double myTotalSize = 0;   // total area of all particles
	private double mySumSqSize = 0;   // sum of squares of particle area (used for the SD)

	private int minSize = 0;       // min particle size
	private int maxSize = 0;       // max particle size

	/**
	 * Constructor.
	 * minCirc and maxCirc are ignored
	 */
	public MyParticleAnalyzer3D(double minSize, double maxSize, double minCirc, double maxCirc) {
		this.minSize = (int)Math.round(minSize);
		this.maxSize = (int)Math.round(maxSize);
	}
	
	/**
	 * Reset all summaries for a new analysis.
	 */
	public void resetSummaries() {
		this.myCount     = 0;
		this.myTotalSize = 0;
	}

	/**
	 * A get for the Particle Count
	 *
	 * @returns myCount
	 */
	public int getCount() {
		return this.myCount;
	}

	/**
	 * A get for the Total Area
	 *
	 * @returns TotalSize
	 */
	public double getTotalSize() {
		return this.myTotalSize;
	}

	/**
	 * Calculate the mean area
	 *
	 * @returns   mean area
	 */
	public double getSizeMean() {
		if (this.myCount == 0) return Double.NaN;
		return this.myTotalSize / this.myCount;
	}

	/**
	 * Calculate the SD of the particle area
	 *
	 * @returns   SD
	 */
	public double getAreaSD() {
		if (this.myCount < 2) return Double.NaN;
		return (this.myCount * this.mySumSqSize - this.myTotalSize * this.myTotalSize) / this.myCount / (this.myCount - 1);
	}

	/**
	 * The main analyze routine.
	 * Analogous to analyze() from ParticleAnalyzer
	 */
	public void analyze(ImagePlus img) {
		Counter3D myCounter = new Counter3D(img, 1, this.minSize, this.maxSize, true, false);
		Vector<?> allObjects = (Vector<?>)myCounter.getObjectsList();

		this.myCount = allObjects.size();
		for (int i = 0; i < this.myCount; i++) {
			Object3D currObj = (Object3D)allObjects.get(i);
			this.myTotalSize += currObj.size;
			this.mySumSqSize += currObj.size * currObj.size;
		}
	}

}
