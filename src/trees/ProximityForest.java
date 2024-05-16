package trees;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.AppContext;
import core.ProximityForestResult;
import core.contracts.Dataset;
import util.PrintUtilities;
import java.util.concurrent.*;

/**
 * 
 * @author shifaz
 * @email ahmed.shifaz@monash.edu
 *
 */

public class ProximityForest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1183368028217094381L;
	protected transient ProximityForestResult result;
	protected int forest_id;
	protected boolean bootstrap;
	protected ProximityTree trees[];
	public String prefix;
	
	Integer[][] num_votes;
	List<Integer> max_voted_classes;
	
	public ProximityForest(int forest_id) {
		this.result = new ProximityForestResult(this);
		
		this.forest_id = forest_id;
		this.trees = new ProximityTree[AppContext.num_trees];
		
		for (int i = 0; i < AppContext.num_trees; i++) {
			trees[i] = new ProximityTree(i, this);
		}

		this.bootstrap = false;

	}

	public ProximityForest(int forest_id, boolean bootstrap) {
		this.result = new ProximityForestResult(this);
		
		this.forest_id = forest_id;
		this.trees = new ProximityTree[AppContext.num_trees];
		
		for (int i = 0; i < AppContext.num_trees; i++) {
			trees[i] = new ProximityTree(i, this);
		}

		this.bootstrap = bootstrap;

	}

	public void train(Dataset train_data) throws Exception {
		result.startTimeTrain = System.nanoTime();
	
		ExecutorService executor = Executors.newFixedThreadPool(AppContext.num_trees);
		List<Future<?>> futures = new ArrayList<>();
	
		for (int i = 0; i < AppContext.num_trees; i++) {
			final int treeIndex = i;
			futures.add(executor.submit(() -> {
				try {
					// synchronized (trees) { // Synchronize access to the trees array
					// 	trees[treeIndex].train(train_data.deep_clone());
					// }
					if (this.bootstrap) {
						trees[treeIndex].train(train_data.bootstrap(AppContext.getRand()));
					} else {
						trees[treeIndex].train(train_data);
					}
	
					if (AppContext.verbosity > 0) {
						synchronized (System.out) {
							System.out.print(treeIndex + ".");
							if (AppContext.verbosity > 1) {
								PrintUtilities.printMemoryUsage(true);
								if ((treeIndex + 1) % 20 == 0) {
									System.out.println();
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("Exception while training tree " + treeIndex, e);
				}
			}));
		}
	
		executor.shutdown();
		try {
			if (!executor.awaitTermination((long)1e5, TimeUnit.DAYS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	
		result.endTimeTrain = System.nanoTime();
		result.elapsedTimeTrain = result.endTimeTrain - result.startTimeTrain;
	}
	
	public ProximityForestResult test(Dataset test_data) throws Exception {
		result.startTimeTest = System.nanoTime();

		num_votes = new Integer[AppContext.num_trees][test_data.size()];

		ExecutorService executor = Executors.newFixedThreadPool(AppContext.num_trees);
		List<Future<?>> futures = new ArrayList<>();
	
		for (int i = 0; i < AppContext.num_trees; i++) {
			final int treeIndex = i;
			futures.add(executor.submit(() -> {
				try {
					num_votes[treeIndex] = trees[treeIndex].predict(test_data);
					
					if (AppContext.verbosity > 0) {
						synchronized (System.out) {
							if (treeIndex % AppContext.print_test_progress_for_each_instances == 0) {
								System.out.print("*");
							}				
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("Exception while training tree " + treeIndex, e);
				}
			}));
		}
	
		executor.shutdown();
		try {
			if (!executor.awaitTermination((long)1e5, TimeUnit.DAYS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
		int actual_class;
		int predicted_class;
		Integer [] votes_over_tree = new Integer[AppContext.num_trees];
		for (int i=0; i < test_data.size(); i++) {
			actual_class = test_data.get_class(i);
			for (int j = 0; j < AppContext.num_trees; j++){
				votes_over_tree[j] = num_votes[j][i];
			}
			predicted_class = majority_votes(votes_over_tree);
			if (actual_class != predicted_class){
				result.errors++;
			}else{
				result.correct++;
			}
		}

	
		result.endTimeTest = System.nanoTime();
		result.elapsedTimeTest = result.endTimeTest - result.startTimeTest;

		result.accuracy  = ((double) result.correct) / test_data.size();
		result.error_rate = 1 - result.accuracy;

		return result;
	}
	
	public Integer majority_votes(Integer[] trees_prediction) {
		// Map to store the count of each label
		Map<Integer, Integer> labelCountMap = new HashMap<>();

		// Count the occurrences of each label
		for (Integer label : trees_prediction) {
			labelCountMap.put(label, labelCountMap.getOrDefault(label, 0) + 1);
		}

		// Find the label with the maximum count
		int maxCount = 0;
		Integer majorityLabel = null;
		for (Map.Entry<Integer, Integer> entry : labelCountMap.entrySet()) {
			int count = entry.getValue();
			if (count > maxCount) {
				maxCount = count;
				majorityLabel = entry.getKey();
			}
		}

		return majorityLabel;
	}
	
	public ProximityTree[] getTrees() {
		return this.trees;
	}
	
	public ProximityTree getTree(int i) {
		return this.trees[i];
	}

	public ProximityForestResult getResultSet() {
		return result;
	}

	public ProximityForestResult getForestStatCollection() {
		
		result.collateResults();
		
		return result;
	}

	public int getForestID() {
		return forest_id;
	}

	public void setForestID(int forest_id) {
		this.forest_id = forest_id;
	}




	
}
