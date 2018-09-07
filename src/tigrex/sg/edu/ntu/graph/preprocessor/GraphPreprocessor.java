package tigrex.sg.edu.ntu.graph.preprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tigrex.sg.edu.ntu.utility.FileUtility;


/**
 * This class reads a temporal graph (assuming directed) and normalizes the id
 * of all vertices (to be from 0 to |V| - 1) and timestamps (to be from 0 to |T|
 * - 1). Vertex with higher outgoing degrees will have smaller new id.
 * Duplicated edges and self-looping edges will be removed.
 * 
 * 
 * The new graph with updated vertex id and timestamp will be written to the
 * same path with added ".sim" extension. The mapping will be written as well,
 * with an extension ".newId" and ".newTime".
 *
 * @author Tigrex
 *
 */
public class GraphPreprocessor {
	
	final private Logger logger = LoggerFactory.getLogger(GraphPreprocessor.class);
	
	private FileUtility fileUtility = new FileUtility();

	private List<List<String>> lines;
	private Map<String, Map<String, String>> rawEdges;
	private Map<String, Integer> vertexIdMapping;
	private Map<String, Integer> timestampMapping;
	
	private Map<Integer, Map<Object, Object>> normalizedGraph;
	
	/**
	 * Assume the file is CSV formatted.
	 * 
	 * @param path
	 */
	public void readFile(String path) {
		this.lines = fileUtility.readCSV(path, 3);
		this.process(path);
	}

	/**
	 * Read a file using the specified delimiter.
	 * 
	 * @param path
	 * @param delimiter
	 */
	public void readFile(String path, String delimiter) {
		this.lines = fileUtility.readFile(path, delimiter, 3);
		this.process(path);
	}

	private void process(String path) {
		this.removeDuplicateAndSelfLoops();
		this.setVertexIdMapping();
		this.setTimestampMapping();
		fileUtility.writeMapToFile(this.vertexIdMapping, path + ".newId");
		fileUtility.writeMapToFile(this.timestampMapping, path + ".newTime");
		
		this.generateNormalizedGraph();
		fileUtility.writeMapOfMapToFile(this.normalizedGraph, path + ".sim");
	}
	
	/**
	 * Remove duplicated edges and self-looping edges. Given a source and a destination, there can be only
	 * a single associated time. If the rule is violated, keep the earliest time.
	 */
	private void removeDuplicateAndSelfLoops() {
		
		logger.debug("+removeDuplicate()");
		logger.info("Previous number of edges is {}.", this.lines.size());
		
		this.rawEdges = new HashMap<String, Map<String, String>>();
		
		int edgeCount = 0;
		for (List<String> line: lines) {
			
			String source = line.get(0);
			String destination = line.get(1);
			String time = line.get(2);
			
			if (source.equals(destination)) {
				continue;
			}
			
			if (!this.rawEdges.containsKey(source)) {
				Map<String, String> outgoingEdges = new HashMap<String, String>(); 
				outgoingEdges.put(destination, time);
				this.rawEdges.put(source, outgoingEdges);
				edgeCount++;
			} else {
				Map<String, String> outgoingEdges = this.rawEdges.get(source);
				if (outgoingEdges.containsKey(destination)) {
					
					String previousTime = outgoingEdges.get(destination);
					
					// Keep a smaller(older) time 
					if (time.compareTo(previousTime) < 0) {
						outgoingEdges.put(destination, time);
					}
					
				} else {
					outgoingEdges.put(destination, time);
					edgeCount++;
				}
			}
			
		}
		
		logger.info("New number of edges is {}.", edgeCount);
		logger.debug("-removeDuplicate()");
	}
	
	private void setVertexIdMapping() {

		logger.debug("+setVertexIdMapping()");

		this.vertexIdMapping = new HashMap<String, Integer>(this.rawEdges.size() / 2);
		
		Set<String> allVertices = new HashSet<String>();
		Set<String> verticesWithOutgoingEdges = new HashSet<String>();
		
		List<VertexDegreeComparator> degrees = new ArrayList<VertexDegreeComparator>();
		
		this.logger.debug("Raw edges size is {}.", rawEdges.size());
		
		for (String source: this.rawEdges.keySet()) {
			int outDegree = this.rawEdges.get(source).size();
			degrees.add(new VertexDegreeComparator(source, outDegree));
			
			allVertices.add(source);
			verticesWithOutgoingEdges.add(source);
			allVertices.addAll(this.rawEdges.get(source).keySet());
		}

		allVertices.removeAll(verticesWithOutgoingEdges);
		for (String vertex: allVertices) {
			degrees.add(new VertexDegreeComparator(vertex, 0));
		}
		
		Collections.sort(degrees);
		Collections.reverse(degrees);
		
		this.logger.debug("Degrees array size is {}.", degrees.size());
		
		for (int i = 0; i < degrees.size(); i++) {
			this.vertexIdMapping.put(degrees.get(i).getVertex(), i);
		}
		
		logger.debug("-setVertexIdMapping()");
	}

	private void setTimestampMapping() {
		logger.debug("-setTimestampMapping()");
		
		this.timestampMapping = new HashMap<String, Integer>();
		
		Set<String> uniqueTimestamps = new HashSet<String>();
		for (String source: this.rawEdges.keySet()) {
			Map<String, String> outgoingEdges = this.rawEdges.get(source);
			uniqueTimestamps.addAll(outgoingEdges.values());
		}
		
		List<String> timestampsList = new ArrayList<String>(uniqueTimestamps.size());
		timestampsList.addAll(uniqueTimestamps);
		
		// TODO Not sort by string, but sort by integer
		Collections.sort(timestampsList);
		
		for (int i = 0; i < timestampsList.size(); i++) {
			this.timestampMapping.put(timestampsList.get(i), i);
		}
		
		logger.debug("-setTimestampMapping()");
	}
	
	
	private void generateNormalizedGraph() {
		this.normalizedGraph = new HashMap<Integer, Map<Object, Object>>();
		
		for (String source: this.rawEdges.keySet()) {
			int sourceIndex = this.vertexIdMapping.get(source);
			
			Map<String, String> outgoingEdges = this.rawEdges.get(source);
			for (String destination: outgoingEdges.keySet()) {
				int destIndex = this.vertexIdMapping.get(destination);
				String time = outgoingEdges.get(destination);
				int timeIndex = this.timestampMapping.get(time);

				if (!this.normalizedGraph.containsKey(sourceIndex)) {
					Map<Object, Object> map = new HashMap<Object, Object>();
					map.put(destIndex, timeIndex);
					this.normalizedGraph.put(sourceIndex, map);
				} else {
					Map<Object, Object> map = this.normalizedGraph.get(sourceIndex);
					map.put(destIndex, timeIndex);
				}
			}
			
		}
		
		
	}

	private static void test() {
		
		System.exit(0);
		// String comparisons
		String str1 = "1234";
		String str2 = "1236";
		System.out.println("(\"1234\").compareTo(\"1236\") = " + str1.compareTo(str2));
		
		// Vertex degree comparisons
		VertexDegreeComparator d1 = new VertexDegreeComparator("a", 1);
		VertexDegreeComparator d2 = new VertexDegreeComparator("b", 4);
		System.out.println("(\"1\").compareTo(\"4\") = " + d1.compareTo(d2));
		List<VertexDegreeComparator> degrees = new ArrayList<VertexDegreeComparator>();
		degrees.add(d1);
		degrees.add(d2);
		Collections.sort(degrees);
		Collections.reverse(degrees);
		System.out.println("After sorting: " + degrees.get(0).getVertex() + " " + degrees.get(1).getVertex());
		
		GraphPreprocessor processor = new GraphPreprocessor();
		processor.readFile("test/graph1.csv");
		
	}
	
	public static void main(String[] args) {
		GraphPreprocessor.test();
	}
	
	
}
