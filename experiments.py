import os
import pathlib
import pandas as pd

if __name__ == '__main__':
    path_to_data = pathlib.Path("/media/aazzari/UCRArchive_2018/")
    ucr = pd.read_csv("paper/ProximityForest_r1.csv", index_col=0, header=0)
    ucr = list(ucr["dataset"])

    datasets = sorted(path_to_data.iterdir())
    datasets = [path for path in datasets if path.is_dir() and path.name in ucr]
    # Iterate over folders in the path
    for i, folder in enumerate(datasets):
        print("Processing ", folder.name)
        # Get the path to the training and testing files
        train_file = folder / (folder.name + "_TRAIN.tsv")
        test_file = folder / (folder.name + "_TEST.tsv")
        # Get the path to the output directory
        out_dir = pathlib.Path("output/" + folder.name)
        # Create the output directory if it does not exist
        if not out_dir.exists():
            out_dir.mkdir(parents=True, exist_ok=True)
        # Exec the Proximity Forest
        command = f"java -jar -Xmx16g ProximityForest.jar -train={train_file} -test={test_file} -out={out_dir} -repeats=10 -trees=100 -r=1 -on_tree=true -export=1 -verbosity=0"
        
        os.system(command)
