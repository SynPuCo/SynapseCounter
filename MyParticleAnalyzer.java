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

import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.measure.*;

public class MyParticleAnalyzer extends ParticleAnalyzer {
	private int myCount = 0;          // total number of particles
	private double myTotalSize = 0;   // total area of all particles
	private double mySumSqSize = 0;   // sum of squares of particle area (used for the SD)

	/**
	 * Constructor.
	 */
	public MyParticleAnalyzer(double minSize, double maxSize, double minCirc, double maxCirc) {
		super(  ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES + 
			ParticleAnalyzer.INCLUDE_HOLES +
			ParticleAnalyzer.SHOW_NONE,
			Measurements.AREA, null, minSize, maxSize, minCirc, maxCirc);
	}

	/**
	 * Reset all summaries for a new analysis.
	 */
	public void resetSummaries() {
		this.myCount     = 0;
		this.myTotalSize = 0;
	}

	/**
	 * Implementation of ParticleAnalyzer.saveResults()
	 *
	 * @param stats the data for the current particle
	 * @param roi   not used
	 */
	protected void saveResults(ImageStatistics stats, Roi roi) {
		this.myCount++;
		this.myTotalSize += stats.area;
		this.mySumSqSize += stats.area * stats.area;
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
	 * @returns TotalArea
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
	public double getSizeSD() {
		if (this.myCount < 2) return Double.NaN;
		return (this.myCount * this.mySumSqSize - this.myTotalSize * this.myTotalSize) / this.myCount / (this.myCount - 1);
	}

}
