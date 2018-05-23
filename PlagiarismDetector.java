

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

/*
 * SD2x Homework #11
 * Improve the efficiency of the code below according to the guidelines in the assignment description.
 * Please be sure not to change the signature of the detectPlagiarism method!
 * However, you may modify the signatures of any of the other methods as needed.
 */

public class PlagiarismDetector {
	
	protected static Map<String, Set<String>> phraseMemo = new HashMap<String, Set<String>>(); // memo map for tracking phrase, giving ~65% improvement

	public static Map<String, Integer> detectPlagiarism(String dirName, int windowSize, int threshold) {
		File dirFile = new File(dirName);
		String[] files = dirFile.list();
		
		/*Map<String, Integer> numberOfMatches = new HashMap<String, Integer>();*/
		Map<Integer, Set<String>> mapNumMatchesToKey = new HashMap();
		Queue<Integer> numMatchesQueue = new PriorityQueue();
		Set<Integer> numMatchesSet = new HashSet();
		Set<String> keySet = new HashSet();
		
		for (int i = 0; i < files.length; i++) {
			String file1 = files[i];
			Set<String> file1Phrases = getPhrases(dirName + "/" + file1, windowSize); // now calls memo wrapper for createPhrases
			if (file1Phrases == null) {
				return null;
			}

			for (int j = 0; j < files.length; j++) { 
				String file2 = files[j];
				if (file1.equals(file2) || keySet.contains(file2 + "-" + file1)) { // moved this conditional to beginning of inner loop instead of end, so that extra instructions are not needlessly carried out, ~5% improvement
					continue;
				}
				
				/*Set<String> file1Phrases = createPhrases(dirName + "/" + file1, windowSize);*/ // moved to outer loop, giving ~23% improvement
				Set<String> file2Phrases = getPhrases(dirName + "/" + file2, windowSize); // now calls memo wrapper for createPhrases
				
				if (file2Phrases == null) // moved null check for file1Phrases to outer loop, negligible improvement
					return null;
				
				Set<String> matches = findMatches(file1Phrases, file2Phrases);
				
				if (matches == null)
					return null;
							
				/*
				if (matches.size() > threshold) {
					String key = file1 + "-" + file2;
						numberOfMatches.put(key,matches.size());
				}
				*/
				
				int numMatches = matches.size();
				if (matches.size() > threshold) {
					if (!numMatchesSet.contains(numMatches)) {
						numMatchesSet.add(numMatches);
						numMatchesQueue.add(-numMatches);
						mapNumMatchesToKey.put(numMatches, new HashSet());
					}
					Set<String> keySetTmp = mapNumMatchesToKey.get(numMatches);
					String key = file1 + "-" + file2;
					keySetTmp.add(key);
					keySet.add(key);
				}
			}
			
		}		
		
		/*
		return sortResults(numberOfMatches);
		*/
		return sortResults2(mapNumMatchesToKey, numMatchesQueue); // call new method for sorting results that sorts based on queue of integers, small improvement

	}

	
	/*
	 * This method reads the given file and then converts it into a Collection of Strings.
	 * It does not include punctuation and converts all words in the file to uppercase.
	 */
	protected static List<String> readFile(String filename) {
		if (filename == null) return null;
		
		List<String> words = new ArrayList<String>(); // changed from linked list to array list because createPhrases method accesses list by index; negligible improvement
		
		try {
			Scanner in = new Scanner(new File(filename));
			while (in.hasNext()) {
				words.add(in.next().replaceAll("[^a-zA-Z]", "").toUpperCase());
			}
			in.close(); // not part of original file
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return words;
	}
	
	/*
	 * This method is a wrapper for createPhrases.  It first checks to see if the phrases for
	 * the file argument exist in a memoized list, then, if not, calls createPhrases.
	 */
	protected static Set<String> getPhrases(String filename, int window) {
		if (phraseMemo.keySet().contains(filename)) {
			return phraseMemo.get(filename);
		}
		else {
			return createPhrases(filename, window);
		}
	}
	
	/*
	 * This method reads a file and converts it into a Set/List of distinct phrases,
	 * each of size "window". The Strings in each phrase are whitespace-separated.
	 */
	protected static Set<String> createPhrases(String filename, int window) {
		if (filename == null || window < 1) return null;
				
		List<String> words = readFile(filename);
		
		Set<String> phrases = new HashSet<String>();
		
		for (int i = 0; i < words.size() - window + 1; i++) {
			String phrase = "";
			for (int j = 0; j < window; j++) {
				phrase += words.get(i+j) + " ";
			}

			phrases.add(phrase);

		}
		
		phraseMemo.put(filename, phrases); // added instruction to enter phrase into memo
		
		return phrases;		
	}

	

	
	/*
	 * Returns a Set of Strings that occur in both of the Set parameters.
	 * However, the comparison is case-insensitive.
	 */
	protected static Set<String> findMatches(Set<String> myPhrases, Set<String> yourPhrases) {
	
		Set<String> matches = new HashSet<String>();
		
		if (myPhrases != null && yourPhrases != null) {
		
			for (String mine : myPhrases) {
				if (yourPhrases.contains(mine)) matches.add(mine); // instead of iterating through yourPhrases, changed to using hashing method to see if mine is in yourPhrases.  This was a HUGE improvement (99.3% improvement).
				/*
				for (String yours : yourPhrases) {
					if (mine.equalsIgnoreCase(yours)) {
						matches.add(mine);
					}
				}
				*/
			}
		}
		return matches;
	}
	
	/*
	 * Returns a LinkedHashMap in which the elements of the Map parameter
	 * are sorted according to the value of the Integer, in non-ascending order.
	 */
	protected static LinkedHashMap<String, Integer> sortResults(Map<String, Integer> possibleMatches) {
		
		// Because this approach modifies the Map as a side effect of printing 
		// the results, it is necessary to make a copy of the original Map
		Map<String, Integer> copy = new HashMap<String, Integer>();
		
		for (String key : possibleMatches.keySet()) {
			copy.put(key, possibleMatches.get(key));
		}	
		
		LinkedHashMap<String, Integer> list = new LinkedHashMap<String, Integer>();

		for (int i = 0; i < copy.size(); i++) {
			int maxValue = 0;
			String maxKey = null;
			for (String key : copy.keySet()) {
				if (copy.get(key) > maxValue) {
					maxValue = copy.get(key);
					maxKey = key;
				}
			}
			
			list.put(maxKey, maxValue);
			
			copy.put(maxKey, -1);
		}

		return list;
	}
	
	/*
	 * Returns a LinkedHashMap in which the elements of the Map parameter
	 * are sorted according to the value of the Integer, in non-ascending order.
	 */
	protected static LinkedHashMap<String, Integer> sortResults2(Map<Integer, Set<String>> mapNumMatchesToKey, Queue<Integer> numMatchesQueue) {
		
		LinkedHashMap<String, Integer> list = new LinkedHashMap();
		while (!(numMatchesQueue.peek() == null)) {
			int numMatches = -(numMatchesQueue.remove());
			Set<String> keySet = mapNumMatchesToKey.get(numMatches);
			for (String key : keySet) {
				list.put(key, numMatches);
			}
		}
		return list;
	}
	
	/*
	 * This method is here to help you measure the execution time and get the output of the program.
	 * You do not need to consider it for improving the efficiency of the detectPlagiarism method.
	 */
    public static void main(String[] args) {
    	if (args.length == 0) {
    		System.out.println("Please specify the name of the directory containing the corpus.");
    		System.exit(0);
    	}
    	String directory = args[0];
    	long start = System.currentTimeMillis();
    	Map<String, Integer> map = PlagiarismDetector.detectPlagiarism(directory, 4, 5);
    	long end = System.currentTimeMillis();
    	double timeInSeconds = (end - start) / (double)1000;
    	System.out.println("Execution time (wall clock): " + timeInSeconds + " seconds");
    	Set<Map.Entry<String, Integer>> entries = map.entrySet();
    	for (Map.Entry<String, Integer> entry : entries) {
    		System.out.println(entry.getKey() + ": " + entry.getValue());
    	}
    }

}
