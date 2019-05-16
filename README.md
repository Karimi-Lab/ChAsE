<img align="left" src="doc/ChaseLogo.jpg" alt="ChAsE logo" height="90"> </img>

# ChAsE: Chromatin Analysis and Exploration Tool

ChAsE is a cross platform desktop application that provides an interactive graphical interface for analysis of epigenomic data. 

Features include:

* Exploration and visualization of the data using an interactive heat map and plot interface.
* Clustering the data automatically or manually by sorting and brushing the heat map.
* Set construction based on presence/absence of signal.
* Ability to compare different clusterings via set operations.
* Exporting results for downstream analysis or as high quality images for publications.

Please check the [official website](http://chase.cs.univie.ac.at) for updated documentation and video tutorials.

## Quick Start

### Installation
This software requires Java 7 or greater. It is recommended that you get the latest version of [Java SE](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

* Download and extract the [latest binary distribution (v 1.1.2)](https://github.com/hyounesy/ChAsE/raw/master/dist/ChAsE_1.1.2.zip) if you don't want to build it yourself:

  * **Windows**: Double click ```Run_Windows.bat```.
  * **Mac**: Right click ```Run_OSX.command``` and select Open. Confirm running the application.
  * **Linux**: Double click ```Run_Unix.sh```.
  * Or, you may open a console and run the following in the command-line: ``` java -jar chase.jar ```

### Example Data
 * [Example Data (full)](http://bigwigs.brc.ubc.ca/bigwig/chase/): Contains a GFF file for Refseq genes (mm9) and several WIG and BIGWIG files (ChIP-seq, RNA-seq and Bis-seq) which can be used to create a new workspace from scratch.
 * [Example Data (small)](https://github.com/hyounesy/ChAsE/raw/master/dist/ExampleData.zip): Contains an example GFF file (mm9) and four small WIG files (ChIP-seq and RNA-seq) which can be used to create a new workspace from scratch.
 * [Example Workspace](https://github.com/hyounesy/ChAsE/raw/master/dist/ExampleWorkspace.zip): Contains the two pre-processed exmaple workspaces for the small and the full example data above, which can be readily opened using the [Open Existing Analysis] option.

## Documentation
Further Documentation, example data and video walkthroughs are available in the official website: [chase.cs.univie.ac.at](http://chase.cs.univie.ac.at/)

## Credits
ChAsE is developed by [Hamid Younesy](https://www.researchgate.net/profile/Hamid_Younesy) under the supervision of [Torsten Möller](https://cs.univie.ac.at/vda-team/infpers/Torsten_M%C3%B6ller/), and in close collaboration with [Cydney Nielsen](http://www.cydney.org/). Special thanks to [Mohammad Karimi](http://brc.ubc.ca/research/computational-biology-and-bioinformatics/), [Matthew Lorincz](http://medgen.med.ubc.ca/person/matthew-lorincz/), [Rebecca Cullum](http://www.terryfoxlab.ca/people-detail/rebecca-cullum/), [Olivia Alder](https://www.researchgate.net/profile/Olivia_Alder), [Bradford Hoffman](https://cfri.ca/our-research/researchers/results/Details/brad-hoffman) and [Arthur Kirkpatrick](http://www.cs.sfu.ca/~ted/) for their feedback and help evaluating this tool.
