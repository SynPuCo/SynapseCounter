# SynapseCounter

## Introduction

Synapse Counter plug-in for ImageJ, developed by Egor Dzyubenko and Andrey Rozenberg, is a helpful scientific tool designed for synapse formation studies. It is developed for rapid, automatic, unbiased quantification of synaptic puncta revealed by synaptic marker proteins fluorescence. You will find some helpful guidelines about Synapse Counter usage below. In case you have additional questions, do not hesitate to contact us by e-mail: [egor.dzyubenko@rub.de](mailto:egor.dzyubenko@rub.de)

## Installation and requirements

The easiest option is to download the latest pre-compiled [release](https://github.com/SynPuCo/SynapseCounter/releases) and copy the jar file in the plugins folder of ImageJ/Fiji (e.g. `/opt/Fiji.app/plugins/`).

If you want to compile the code your self, clone the repository (`git clone https://github.com/SynPuCo/SynapseCounter.git`) or download it from your browser as [zip](https://github.com/SynPuCo/SynapseCounter/archive/master.zip). Compilation goes as follows:

	name=Synapse_Counter
	plugins_dir=/opt/Fiji.app/plugins/ # or other relevant location
	javac "$name.java" MyParticleAnalyzer.java -cp /opt/Fiji.app/jars/*:.
	jar -cf "$name.jar" $name.class MyParticleAnalyzer.class plugins.config
	mv "$name.jar" 
	rm *.class

Either way the plugin is than available under Plugins → Analyze → Synapse Counter

This plug-in is supported by ImageJ versions starting 1.48, as it exploits the functions Auto Threshold and Watershed, which may not be present in earlier versions.

## Recommended formats

The plug-in supports RGB and multichannel image formats, which are supported by ImageJ. This includes png, bmp, jpeg, tiff, lsm and some others. However, we recommend to use tiff format or the raw multichannel images from your microscope (like lsm for Carl Zeiss microscopes), because these images are not altered due to data compression. If you are using compressed picture formats (like png), please pay attention to use images of same formats throughout your experiments, as the outcome of quantification might subtly depend on compression method. The plug-in will not read database files or stacked images.

## How it works?

Synapse Counter is designed to quantify synaptic puncta. Synaptic puncta are dot-like synapse associated structures, identified by synaptic marker proteins fluorescence. They can be detected as presynaptic and postsynaptic marker protein clusters, which are formed during synapse formation and maturation. This method requires the pairs of presynaptic and postsynaptic proteins to be fluorescently tagged. Synapse Counter will automatically quantify the number and size of presynaptic and postsynaptic protein clusters. It will also identify and quantify the co-localizing puncta, which are highly potent to represent structurally accomplished synapses.

## Image processing and puncta quantification parameters

Synapse Counter is a highly adjustable plug-in. If the default parameters that we found to be optimal in our experiments (that is: correspondent to by-hand user supervised quantification) are not suitable in some cases, the user can define them individually. In this section we will review the tools that are available in the Synapse Counter dialog window. These tools are divided into General, Image Processing and Analyze Particles groups. After changing the parameters, they will be preserved for the next usage. To go back to the default parameters, press the “Reset to defaults” button.

### General

#### Default parameters

The default parameters are defined for 1024x1024 pixel images and are suitable for the majority of applications. These parameters are optimal for glutamatergic and gabaergic synapse quantification using VGlut1-PSD95 and VGAT-Gephyrin pairs respectively.

#### Choose input source

This tool allows to process either currently open images one by one (tick “current image”), or to process the entire batch by choosing the correspondent folder (tick “batch mode”). If you want the program to search the images in subfolders, tick “search in subfolders”.

#### Save intermediate files

If you want to check the image processing algorithm for possible artifacts, select this option to have the access to intermediate files. The files will be stored in a user-specified output folder. Intermediate files are necessary to optimize image processing and quantification parameters if needed.

#### Image type

Allows to switch between the analysis of multichannel and RGB types of images. Select the type you will use.

#### Presynaptic and postsynaptic protein channel

Select the channels which your marker proteins belong to (channel number for multichannel images, color for RGB images)

#### Resize image width

The default parameters are identified for 1024x1024 pixel images. In case you have other resolution, image resizing may be helpful to avoid all other parameters optimization (see below).

### Image processing

General note: after changing a parameter from this set, the user should manually verify whether the generated binary puncta images correspond to the real synaptic puncta of the source image. We recommend to use “current image” mode for such optomizations

#### Rolling ball radius
This parameter is used for automatic background subtraction by the “Subtract Background” tool of ImageJ. Too small rolling ball radius will lead to the signal loss and will significantly slow down the quantification.

#### Maximum filter radius

To refine the synaptic puncta, Synapse Counter exploits the built-in Maximum Filter, which rounds the synaptic puncta and helps to distinguish them from the background. The values of 1 or 2 pixels are recommended for the images not exceeding 2048x2048 pixels resolution. Higher values may lead to unreliable puncta detection and artifacts.

#### Method for threshold adjustment

Synapse Counter exploits the built-in Auto Threshold function of ImageJ, which provides a vast spectrum of automatic threshold setting methods. Otsu method, used as a default, implements automatic clustering of background and foreground pixels to set the image threshold basing on the intra-class variance. The user can select an alternative method of automatic threshold setting to create a binary image. To find the optimal method, we recommend to run several test images in the “current image” mode with different automatic thresholding methods. Then the user should manually verify whether the generated binary puncta images correspond to the real synaptic puncta of the source image.

### Analyze Particles

The parameters of the built-in ImageJ Analyse Particles function are introduced to overcome the possible artifacts coming from the high noise or staining artifacts. The user can define minimal and maximum size of presynaptic and postsynaptic proteins puncta that correspond to specific staining. The results will be then generated regarding only the particles which size in within the defined interval. 
To optimize the parameters of puncta quantification for a particular case, we recommend the following:

1. Run a few test images (3 per condition) with Synapse Counter plug-in using the “batch mode” option, while having the “Save intermediate files” activated;
2. Open the intermediate files – they contain binary images with presynaptic and postsynaptic channels;
3. Select the smallest puncta in the presynaptic channel binary image, which corresponds to the real synaptic puncta of the source image, with the “magic wand” tool, run “measure”. Important: remove the scale before measuring (Analyse->Set Scale->tick “Remove scale”+”Global”). Repeat the procedure for all test images. The mean value of the obtained “area” results will give a good estimate for the “Min presynaptic particle size” value.
4. Select the biggest puncta in the presynaptic channel binary image, which corresponds to the real synaptic puncta of the source image, with the “magic wand” tool, run “measure”. Repeat the procedure for all test images. The mean value of the obtained “area” results will give a good estimate for the “Max presynaptic particle size” value
5. Repeat steps iii and iv for the postsynaptic channel
The minimal size of colocalized puncta is automatically defined as 1/3 of the minimal “Min particle size” value, maximal size is defined as the maximal “Max particle size” value. This estimate bases on the idea that presynaptic and postsynaptic puncta should be overlapped by 33-100% to be considered as colocalized. Thus, this parameter should be in principle of no need to adjust.
