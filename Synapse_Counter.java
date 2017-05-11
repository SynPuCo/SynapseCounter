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

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.io.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Math.*;

public class Synapse_Counter implements PlugIn, ActionListener, DialogListener, ItemListener {

	private GenericDialog gd;                                    // dialog to show our options
	private Panel p;                                             // container panel
	private String type;                                         // image type
	private String preChannelTag, posChannelTag;                 // pre- and post-synaptic channels
	private double rollBallRad, maxFiltRad;                      // rolling ball rad. for background subtraction and rad. for "maximum" filter
	private String threshMethod;                                 // auto threshold method
	private double minSizePre, maxSizePre;                       // min/max particle sizes
	private double minSizePos, maxSizePos;                       //
	private int resizeWidth;                                     // resize width [0 = no resize]
	private TextField inputDirField, outputDirField;             // TextField's for folder choice
	private String inputDir, outputDir;                          // folders
	private boolean is3d;                                        // 2d/3d flag
	private Button inputButton, outputButton, resetButton;       // buttons for folder choice and the "reset" button

	private String oldType;                                      // for type tracking
	private ImageCalculator imageCalculator;                     // ImageCalculator
	private ResultsTable resultsTable;                           // table to save the results in
	private MyParticleAnalyzer[] partAnalyzers;                  // array of our MyParticleAnalyzer   instances
	private MyParticleAnalyzer3D[] partAnalyzers3D;              // array of our MyParticleAnalyzer3D instances
	private CheckboxGroup inputBox, dimBox;                      // checkbox for the type of the input source
	private Checkbox doOpenedImageButton, doBatchButton, doSubFoldersButton, is2dButton, is3dButton; // checkbox for the respective switchers
	private boolean doOutput, doOpenedImage, doSubFolders;       // task switchers
	private String[] autoMethods = AutoThresholder.getMethods(); // list of AutoThresholder methods

	public static final String HLINE = "__ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __";
	public static final String HELP = "<html>This program was written by Andrey Rozenberg and Egor Dzyubenko<br/><a href='https://github.com/SynPuCo/SynapseCounter/'>Visit our GitHub page for more details</a>"; // help text

	public static final String[] types          = {"Multi-channel", "RGB"};        // types of input images
	public static final String[] channelChoices = {"C1", "C2", "C3", "C4", "C5"};  // choice of channels for multi-channel images
	public static final String[] colorChoices   = {"green", "blue", "red"};        // choice of channels for RGB images

	// defaults

	public static final String  DEF_type             = types[0];
	public static final double  DEF_rollBallRad      = 10;
	public static final double  DEF_maxFiltRad       = 2;
	public static final String  DEF_threshMethod     = "Otsu";
	public static final double  DEF_minSizePre       = 10;
	public static final double  DEF_maxSizePre       = 400;
	public static final double  DEF_minSizePos       = 10;
	public static final double  DEF_maxSizePos       = 400;
	public static final int     DEF_resizeWidth      = 0;
	public static final String  DEF_preChannelTagRGB = colorChoices[0];
	public static final String  DEF_posChannelTagRGB = colorChoices[1];
	public static final String  DEF_preChannelTag    = channelChoices[0];
	public static final String  DEF_posChannelTag    = channelChoices[2];
	public static final boolean DEF_is3d             = false;

	// default command for AutoThreshold

	private String autoThresholdCmd = "Auto Threshold";
	private boolean autoThresholdCmdChecked = false;

	/**
	 * The main program entry.
	 * <p>
	 * Implementation of PlugIn.run()
	 *
	 * @param  arg  plugin arguments
	 */
	public void run(String arg) {
		while (!validateDialog()) {        // iterate until the dialog is validated
			if (!invokeDialog()) return;   // exit if cancelled
		}
		runSynapseCounter();               // do the job if everything is OK
	}

	/**
	 * The main analysis launcher.
	 * Decides where the images are expected to come 
	 * and initializes our particle analyzers
	 */
	private void runSynapseCounter() {
		resultsTable     = new ResultsTable();
		imageCalculator  = new ImageCalculator();
		double minSize = Math.min(minSizePre, minSizePos) / 3.0;  // colocalization min particle size
		double maxSize = Math.max(maxSizePre, maxSizePos);        // colocalization max particle size
		if (is3d) {
			partAnalyzers3D    = new MyParticleAnalyzer3D[3];
			partAnalyzers3D[0] = new MyParticleAnalyzer3D(minSizePre, maxSizePre, 0.0, 1.0);
			partAnalyzers3D[1] = new MyParticleAnalyzer3D(minSizePos, maxSizePos, 0.0, 1.0);
			partAnalyzers3D[2] = new MyParticleAnalyzer3D(minSize,    maxSize,    0.0, 1.0);
		} else {
			partAnalyzers    = new MyParticleAnalyzer[3];
			partAnalyzers[0] = new MyParticleAnalyzer(minSizePre, maxSizePre, 0.0, 1.0);
			partAnalyzers[1] = new MyParticleAnalyzer(minSizePos, maxSizePos, 0.0, 1.0);
			partAnalyzers[2] = new MyParticleAnalyzer(minSize,    maxSize,    0.0, 1.0);
		}

		if (doOpenedImage) {
			runSynapseCounterOpenedImage();
		}
		else {
			runSynapseCounterBatch("");
		}
		IJ.showProgress(1, 1);
	}

	/**
	 * Launch analysis of images in a (sub)folder.
	 * Recursive if subfolders are to be searched
	 *
	 * @param  subDir sub-directory
	 */
	private void runSynapseCounterBatch(String subDir) {
		Opener myOpener = new Opener();
		myOpener.setSilentMode(true);
		String[]  files = (new File(inputDir + subDir)).list();
		String    file;
		ImagePlus image;
		for (int i = 0; i < files.length; i++) {
			if (IJ.escapePressed()) break;
			file = inputDir + subDir + files[i];
			if (file.startsWith(".")) continue;
			if ((new File(file)).isDirectory()) {
				if (doSubFolders) runSynapseCounterBatch(subDir + files[i] + File.separator);
				continue;
			}
			if (!doSubFolders) {
				IJ.showStatus(i + "/" + files.length);
				IJ.showProgress(i, files.length);
			}
			image = myOpener.openImage(file);
			if (image == null) {
				IJ.log("Couldn't open '" + file + "'");
				continue;
			}
			if (!processImage(image, subDir, files[i])) continue;
		}
	}

	/**
	 * Launch analysis of the image currently opened
	 */
	private void runSynapseCounterOpenedImage() {
		ImagePlus image = WindowManager.getCurrentImage();
		processImage(image, "", image.getTitle());
	}

	/**
	 * The image analysis function itself.
	 *
	 * @param  subDir sub-directory
	 * @return        true if OK, false otherwise
	 */
	private boolean processImage(ImagePlus image, String subDir, String file) {
		int row;
		String title;
		String preTitle;
		String posTitle;
		String fileName = subDir + file;
		if (type.equals("RGB")) {
			preTitle = preChannelTag;
			posTitle = posChannelTag;
		}
		else {
			preTitle = preChannelTag + "-" + file;
			posTitle = posChannelTag + "-" + file;
		}
		ImagePlus[] allChannels = ChannelSplitter.split(image);
		if (!doOpenedImage) removeIMP(image);
		ImagePlus preChannel = null;
		ImagePlus posChannel = null;
		ImagePlus synChannel;
		for (int j = 0; j < allChannels.length; j++) {
			title = allChannels[j].getTitle();
			// IJ.log(title);
			if      (title.equals(preTitle))
				preChannel = allChannels[j];
			else if (title.equals(posTitle))
				posChannel = allChannels[j];
			else
				removeIMP(allChannels[j]);
		}
		if (preChannel == null) {
			IJ.log(fileName + ": channel " + preChannelTag + " not found");
			return false;
		}
		if (posChannel == null) {
			IJ.log(fileName + ": channel " + posChannelTag + " not found");
			return false;
		}
		cleanUp(preChannel);
		cleanUp(posChannel);

		String suffix = is3d ? " stack" : "";
		synChannel = imageCalculator.run("AND create" + suffix, preChannel, posChannel);
		row = resultsTable.getCounter();
		resultsTable.setValue("File", row, fileName);

		ImagePlus[] myChannels = new ImagePlus[] { preChannel, posChannel, synChannel };
		String[]    myPrefixes = new String[]    { "Presyn.",  "Postsyn.", "Coloc."   };
		String[]    myTags     = new String[]    { "presyn",   "postsyn",  "coloc"    };
		int   myCount;
		double sizeMean;
		for (int j = 0; j < 3; j++) {
			if (is3d) {
				partAnalyzers3D[j].resetSummaries();
				partAnalyzers3D[j].analyze(myChannels[j]);
				myCount  = partAnalyzers3D[j].getCount();
				sizeMean = partAnalyzers3D[j].getSizeMean();
			} else {
				partAnalyzers[j].resetSummaries();
				partAnalyzers[j].analyze(myChannels[j]);
				myCount  = partAnalyzers[j].getCount();
				sizeMean = partAnalyzers[j].getSizeMean();
			}
			removeOrShowIMP(myChannels[j], doOpenedImage, doOutput, subDir, file, myTags[j]);
			resultsTable.setValue(myPrefixes[j] + " N",         row, myCount);
			resultsTable.setValue(myPrefixes[j] + " mean size", row, sizeMean);
		}
		resultsTable.show("SynapseCounter results");
		return true;
	}

	/**
	 * Remove channel
	 *
	 * @param  channel  channel to be removed
	 */
	private void removeIMP(ImagePlus channel) {
		if (channel == null) return;
		channel.close();
		channel = null;
	}

	/**
	 * Create a directory
	 *
	 * @param  dir    directory to make
	 * @return        true if OK, false otherwise
	 */
	private boolean makeDir(String dir) {
		if (dir.equals("")) return true;
		String dirToMake = outputDir + dir;
		File myDir = new File(dirToMake);
		boolean result = true;
		if (!myDir.exists()) {
			try {
				myDir.mkdir();
			} 
			catch(SecurityException se){
				result = false;
				IJ.log("Couldn't create " + dirToMake + " subdirectory");
			}
		}
		return result;
	}

	/**
	 * The final step in tracing a channel: remove or show it.
	 *
	 * @param  channel  channel
	 * @param  show     flag to decide, whether the channel is to be shown to the user or removed
	 * @param  save     flag to optionally save the image on disk before doing anything else
	 * @param  subDir   sub-folder for saving
	 * @param  filename file basename for saving
	 * @param  tag      channel file suffix for saving
	 */
	private void removeOrShowIMP(ImagePlus channel, boolean show, boolean save, String subDir, String filename, String tag) {
		if (channel == null) return;
		if (save && makeDir(subDir)) {
			String myTiff = outputDir + subDir + File.separator + filename + "-" + tag + ".tiff";
			new FileSaver(channel).saveAsTiff(myTiff);
		}
		if (show) {
			channel.show();
		}
		else {
			channel.close();
			channel = null;
		}
	}

	/**
	 * Clean up the channel before the analysis.
	 *
	 * @param  channel  the channel
	 */
	private void cleanUp(ImagePlus channel) {
		if (!autoThresholdCmdChecked && Menus.getCommands().get(autoThresholdCmd) == null)
			autoThresholdCmd += "...";
		autoThresholdCmdChecked = true;

		if (resizeWidth > 0)
			IJ.run(channel, "Size...", "width=" + resizeWidth + " constrain average interpolation=Bilinear");
		String suffix = is3d ? " stack" : "";
		IJ.run(channel, "Smooth", suffix);
		IJ.run(channel, "Subtract Background...", "rolling=" + rollBallRad + suffix);
		IJ.run(channel, "Maximum...", "radius=" + maxFiltRad + suffix);
		final double mean = (double)channel.getStatistics(Measurements.MEAN).mean;
		IJ.run(channel, "Subtract...", "value=" + mean + suffix);
		IJ.run(channel, autoThresholdCmd, "method=[" + threshMethod + "] white" + suffix);
		IJ.run(channel, "Make Binary", suffix);
		IJ.run(channel, "Watershed", suffix);
	}

	/**
	 * Check if a (user-specified) folder is valid
	 *
	 * @param  path  the folder
	 * @param  name  type of folder
	 *
	 * @return       true if OK, false otherwise
	 */
	private boolean checkFolder(String path, String name) {
		if (path.equals("")) {
			IJ.error("SynapseCounter", name + " folder not specified");
			return false;
		}
		File testDir = new File(path);
		if (!testDir.exists()) {
			IJ.error("SynapseCounter", name + " folder does not exist");
			return false;
		}
		if (!testDir.isDirectory()) {
			IJ.error("SynapseCounter", name + " is not a folder");
			return false;
		}
		return true;
	}

	/**
	 * Check if the dialog is OK after the user pressed "OK"
	 *
	 * @return       true if OK, false otherwise
	 */
	private boolean validateDialog() {
		if (gd == null || !gd.wasOKed()) return false;
		if (!doOpenedImage && !checkFolder(inputDirField.getText(), "Input")) {
			return false;
		}
		if (doOpenedImage && WindowManager.getCurrentImage() == null) {
			IJ.error("SynapseCounter", "No image is opened");
			return false;
		}
		if (doOutput && !checkFolder(outputDirField.getText(), "Output")) {
			return false;
		}
		if (preChannelTag == posChannelTag) {
			IJ.error("SynapseCounter", "The two channels are not allowed to be identical");
			return false;
		}
		return true;
	}

	/**
	 * Create the dialog with the options
	 *
	 * @return       true if OK, false otherwise
	 */
	private boolean invokeDialog() {
		if (gd != null) {
			gd.removeAll();
			gd.dispose();
			gd = null;
		}
		gd = new GenericDialog("Analyze synapses");
		gd.addDialogListener(this);
		oldType = "";

		is3d          = Prefs.get("synapsecounter.is3d",          DEF_is3d);
		type          = Prefs.get("synapsecounter.type",          DEF_type);
		doOutput      = Prefs.get("synapsecounter.doOutput",      false);
		doOpenedImage = Prefs.get("synapsecounter.doOpenedImage", false);
		doSubFolders  = Prefs.get("synapsecounter.doSubFolders",  false);

		// gd.addMessage(HLINE);

		inputBox            = new CheckboxGroup();
		doOpenedImageButton = new Checkbox(" current image", inputBox,  doOpenedImage);
		doBatchButton       = new Checkbox(" batch mode:",   inputBox, !doOpenedImage);

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		doOpenedImageButton.addItemListener(this);
		doBatchButton.addItemListener(this);
		p.add(new Label("Choose input source:"));
		p.add(doOpenedImageButton);
		p.add(doBatchButton);
		gd.addPanel(p);

		dimBox              = new CheckboxGroup();
		is2dButton          = new Checkbox(" 2D", dimBox, !is3d);
		is3dButton          = new Checkbox(" 3D", dimBox,  is3d);

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		is2dButton.addItemListener(this);
		is3dButton.addItemListener(this);
		p.add(new Label("Input dimesionality:"));
		p.add(is2dButton);
		p.add(is3dButton);
		gd.addPanel(p);

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		inputButton = new Button("Input folder... ");
		inputButton.addActionListener(this);
		inputButton.setEnabled(!doOpenedImage);
		p.add(inputButton);
		inputDir = Prefs.get("synapsecounter.inputDir", "");
		inputDirField = new TextField(inputDir, 30);
		inputDirField.setEnabled(false);
		p.add(inputDirField);
		gd.addPanel(p);

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		doSubFoldersButton  = new Checkbox(" search in subfolders", null,  doSubFolders);
		p.add(doSubFoldersButton);
		gd.addPanel(p);

		if (doOpenedImage) {
			inputDirField.setText("");
			inputButton.setEnabled(false);
			doSubFoldersButton.setEnabled(false);
		}

		// gd.addMessage(HLINE);
		gd.addCheckbox(" Save intermediate files:", doOutput);

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		outputButton = new Button("Output folder...");
		outputButton.addActionListener(this);
		p.add(outputButton);
		outputDir = Prefs.get("synapsecounter.outputDir", "");
		outputDirField = new TextField(outputDir, 30);
		outputDirField.setEnabled(false);
		p.add(outputDirField);
		gd.addPanel(p);

		if (!doOutput) {
			outputDirField.setText("");
			outputButton.setEnabled(false);
		}

		// gd.addMessage(HLINE);
		gd.addMessage("Analysis settings:");

		gd.addChoice("Image type:", types, type);
		// gd.addCheckbox("3-dimesional input", Prefs.get("synapsecounter.is3d", DEF_is3d));

		gd.addChoice("Presynaptic protein channel:",  new String[]{}, "");
		gd.addChoice("Postsynaptic protein channel:", new String[]{}, "");

		gd.addNumericField("Resize image width:",    Prefs.get("synapsecounter.resizeWidth", DEF_resizeWidth),  0, 6, "px");
		gd.addNumericField("Rolling ball radius:",   Prefs.get("synapsecounter.rollBallRad", DEF_rollBallRad), 1);
		gd.addNumericField("Maximum filter radius:", Prefs.get("synapsecounter.maxFiltRad",  DEF_maxFiltRad),  1);
		gd.addChoice("Method for threshold adjustment:", autoMethods, Prefs.get("synapsecounter.threshMethod", DEF_threshMethod));

		gd.addNumericField("Presynaptic particle size:",       Prefs.get("synapsecounter.minSizePre", DEF_minSizePre), 0, 6, "px² or voxels");
		gd.addNumericField("Max. presynaptic particle size:",  Prefs.get("synapsecounter.maxSizePre", DEF_maxSizePre), 0, 6, "px² or voxels");

		gd.addNumericField("Min. postsynaptic particle size:", Prefs.get("synapsecounter.minSizePos", DEF_minSizePos), 0, 6, "px² or voxels");
		gd.addNumericField("Max. postsynaptic particle size:", Prefs.get("synapsecounter.maxSizePos", DEF_maxSizePos), 0, 6, "px² or voxels");

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		resetButton = new Button("Reset to defaults");
		resetButton.addActionListener(this);
		p.add(resetButton);
		gd.addPanel(p);

		gd.addHelp(HELP);

		switchType(type);

		gd.showDialog();
		if (gd.wasCanceled()) return false;

		type	      = gd.getNextChoice();
		preChannelTag = gd.getNextChoice();
		posChannelTag = gd.getNextChoice();
		resizeWidth   = (int)gd.getNextNumber();
		rollBallRad   = gd.getNextNumber();
		maxFiltRad    = gd.getNextNumber();
		threshMethod  = gd.getNextChoice();
		minSizePre    = gd.getNextNumber();
		maxSizePre    = gd.getNextNumber();
		minSizePos    = gd.getNextNumber();
		maxSizePos    = gd.getNextNumber();
		doSubFolders  = doSubFoldersButton.getState();
		doOutput      = gd.getNextBoolean();
		doOpenedImage = (inputBox.getSelectedCheckbox() == doOpenedImageButton);
		is3d          = (dimBox.getSelectedCheckbox()   == is3dButton);

		if (!inputDirField.getText().equals(""))  inputDir  = inputDirField.getText();
		if (!outputDirField.getText().equals("")) {
			outputDir = outputDirField.getText();
		}
		Prefs.set("synapsecounter.type", type);
		if (type.equals("RGB")) {
			Prefs.set("synapsecounter.preChannelTagRGB", preChannelTag);
			Prefs.set("synapsecounter.posChannelTagRGB", posChannelTag);
		}
		else {
			Prefs.set("synapsecounter.preChannelTag", preChannelTag);
			Prefs.set("synapsecounter.posChannelTag", posChannelTag);
		}
		Prefs.set("synapsecounter.resizeWidth",    resizeWidth  );
		Prefs.set("synapsecounter.rollBallRad",    rollBallRad  );
		Prefs.set("synapsecounter.maxFiltRad",     maxFiltRad   );
		Prefs.set("synapsecounter.threshMethod",   threshMethod );
		Prefs.set("synapsecounter.minSizePre",     minSizePre   );
		Prefs.set("synapsecounter.maxSizePre",     maxSizePre   );
		Prefs.set("synapsecounter.minSizePos",     minSizePos   );
		Prefs.set("synapsecounter.maxSizePos",     maxSizePos   );
		Prefs.set("synapsecounter.inputDir",       inputDir     );
		Prefs.set("synapsecounter.outputDir",      outputDir    );
		Prefs.set("synapsecounter.doOpenedImage",  doOpenedImage);
		Prefs.set("synapsecounter.doSubFolders",   doSubFolders );
		Prefs.set("synapsecounter.doOutput",       doOutput     );
		Prefs.set("synapsecounter.is3d",           is3d         );
		return true;
	}

	/**
	 * Reset default settings for the parameters.
	 */
	private void resetDeafults() {
		Prefs.set("synapsecounter.is3d",             DEF_is3d            );
		Prefs.set("synapsecounter.type",             DEF_type            );
		Prefs.set("synapsecounter.rollBallRad",      DEF_rollBallRad     );
		Prefs.set("synapsecounter.maxFiltRad",       DEF_maxFiltRad      );
		Prefs.set("synapsecounter.threshMethod",     DEF_threshMethod    );
		Prefs.set("synapsecounter.minSizePre",       DEF_minSizePre      );
		Prefs.set("synapsecounter.maxSizePre",       DEF_maxSizePre      );
		Prefs.set("synapsecounter.minSizePos",       DEF_minSizePos      );
		Prefs.set("synapsecounter.maxSizePos",       DEF_maxSizePos      );
		Prefs.set("synapsecounter.resizeWidth",      DEF_resizeWidth     );
		Prefs.set("synapsecounter.preChannelTagRGB", DEF_preChannelTagRGB);
		Prefs.set("synapsecounter.posChannelTagRGB", DEF_posChannelTagRGB);
		Prefs.set("synapsecounter.preChannelTag",    DEF_preChannelTag   );
		Prefs.set("synapsecounter.posChannelTag",    DEF_posChannelTag   );
	}

	/**
	 * Event listener to launch folder choice windows when requested
	 * and reset defaults if the "reset" button was pressed.
	 * <p>
	 * Implementation of ActionListener.actionPerformed()
	 *
	 * @param   e    the event
	 * @return       true if OK, false otherwise
	 */
	public void actionPerformed(ActionEvent e) {
		Object fired = e.getSource();
		if (fired == inputButton) {
			String path = IJ.getDirectory("Input Folder");
			if (path == null) return;
			inputDirField.setText(path);
		}
		else if (fired == outputButton) {
			String path = IJ.getDirectory("Output Folder");
			if (path != null) outputDirField.setText(path);
		}
		else if (fired == resetButton) {
			resetDeafults();
			invokeDialog();
		}
	}

	/**
	 * Re-populate the channel choice drop-down if the image type has been changed
	 *
	 * @param newType  the newly specified image type
	 */
	private void switchType(String newType) {
		if (newType.equals(oldType)) return;
		Choice preChoice = (Choice)gd.getChoices().get(1);
		Choice posChoice = (Choice)gd.getChoices().get(2);
		preChoice.removeAll();
		posChoice.removeAll();
		String preChosen;
		String posChosen;
		String[] itemsList;
		if (newType.equals("RGB")) {
			preChosen = Prefs.get("synapsecounter.preChannelTagRGB", DEF_preChannelTagRGB);
			posChosen = Prefs.get("synapsecounter.posChannelTagRGB", DEF_posChannelTagRGB);
			itemsList = colorChoices;
		}
		else {
			preChosen = Prefs.get("synapsecounter.preChannelTag", DEF_preChannelTag);
			posChosen = Prefs.get("synapsecounter.posChannelTag", DEF_posChannelTag);
			itemsList = channelChoices;
		}
		for (int i = 0; i < itemsList.length; i++) {
			preChoice.add(itemsList[i]);
			posChoice.add(itemsList[i]);
		}
		preChoice.select(preChosen);
		posChoice.select(posChosen);
		oldType = newType;
	}

	/**
	 * Another event listener to apply relevant changes to the dialog 
	 * depending on user-specified options
	 * <p>
	 * Implementation of DialogListener.dialogItemChanged()
	 *
	 * @param   e    the event
	 * @return       true if OK, false otherwise
	 */
	public boolean dialogItemChanged(GenericDialog myGd, AWTEvent e) {
		if (e == null) return true;
		switchType(myGd.getNextChoice());
		doOutput     = myGd.getNextBoolean();
		if (doOutput) {
			outputDirField.setText(outputDir);
			outputButton.setEnabled(true);
		}
		else {
			outputDir = outputDirField.getText();
			outputDirField.setText("");
			outputButton.setEnabled(false);
		}
		return true;
	}

	/**
	 * Yet another event listener to apply relevant changes to the dialog 
	 * depending on user-specified options
	 * <p>
	 * Implementation of ItemListener.itemStateChanged()
	 *
	 * @param   e    the event
	 */
	public void itemStateChanged(ItemEvent e) {
		if (inputBox.getSelectedCheckbox() == doBatchButton) {
			inputDirField.setText(inputDir);
			inputButton.setEnabled(true);
			doSubFoldersButton.setEnabled(true);
		}
		else {
			inputDir = inputDirField.getText();
			inputDirField.setText("");
			inputButton.setEnabled(false);
			doSubFoldersButton.setEnabled(false);
		}
	}
}
