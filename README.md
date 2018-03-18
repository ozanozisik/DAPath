# DAPath
DAPath is a Cytoscape app for discovering signaling paths and the pathways that contain these paths which are subjected to cumulative impact of modestly associated variants. The parameters of DAPath are:
- KEGG folder: Path of the folder that containes KEGG pathway files
- Experiment file: Path of the experiment file that contains gene name p-value pairs
- Output folder: Path of the folder that result files will be stored
- Whether to skip first line of experiment file
- Number of top pathways to visualize
- Whether to apply crosstalk handling and the tolerance for crosstalk

Sample KEGG folder and experiment file are provided. 
The KEGG folder must contain hsaGeneIdsInPathways.txt and hsaGenes.txt files.
