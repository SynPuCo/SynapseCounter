import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.measure.*;

public class MyParticleAnalyzer extends ParticleAnalyzer {
	private int myCount = 0;          // total number of particles
	private double myTotalArea = 0;   // total area of all particles
	private double myMeanArea  = 0;   // mean area
	private double mySumSqArea = 0;   // sum of squares of particle area (used for the SD)

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
		this.myTotalArea = 0;
		this.myMeanArea  = 0;
	}

	/**
	 * Implementation ParticleAnalyzer.saveResults()
	 *
	 * @param stats the data the current particle
	 * @param roi   not used
	 */
	protected void saveResults(ImageStatistics stats, Roi roi) {
		this.myCount++;
		this.myTotalArea += stats.area;
		this.mySumSqArea += stats.area * stats.area;
	}

	/**
	 * A get for the particle Count
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
	public double getTotalArea() {
		return this.myTotalArea;
	}

	/**
	 * Calculate the mean area
	 *
	 * @returns   mean area
	 */
	public double getAreaMean() {
		if (this.myCount == 0) return Double.NaN;
		return this.myTotalArea / this.myCount;
	}

	/**
	 * Calculate the SD of the particle area
	 *
	 * @returns   SD
	 */
	public double getAreaSD() {
		if (this.myCount < 2) return Double.NaN;
		return (this.myCount * this.mySumSqArea - this.myTotalArea * this.myTotalArea) / this.myCount / (this.myCount - 1);
	}

}
