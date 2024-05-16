import pathlib
import numpy as np
import pandas as pd
import json


path_to_data = pathlib.Path("output/")
ucr = pd.read_csv("paper/ProximityForest_r1.csv", index_col=0, header=0)
acc_pf = ucr.iloc[:, 7].to_numpy()
ucr = list(ucr["dataset"])
datasets = sorted(path_to_data.iterdir())
datasets = [path for path in datasets if path.is_dir() and path.name in ucr]

acc = np.zeros((len(datasets), 10))

for i, folder in enumerate(datasets):
    runs = list(folder.iterdir())
    for j, path in enumerate(runs):
        with open(path) as f:
            data = json.load(f)
            acc[i, j] = data["accuracy"]

acc = pd.DataFrame(acc.mean(axis=1), index=[x.stem for x  in datasets])
acc_pf = pd.DataFrame(acc_pf, index=ucr)

acc = pd.concat([acc, acc_pf], axis=1, join="inner")
acc.columns = ["ProximityForest", "ProximityForest_r1"]
acc.to_csv("output/compare.csv")



    

