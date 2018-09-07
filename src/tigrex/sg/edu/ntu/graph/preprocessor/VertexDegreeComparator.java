package tigrex.sg.edu.ntu.graph.preprocessor;

public class VertexDegreeComparator implements Comparable<VertexDegreeComparator> {
	private String vertex;
	private Integer degree;
	
	public VertexDegreeComparator(String vertex, int degree) {
		this.vertex = vertex;
		this.degree = degree;
	}
	
	public String getVertex() {
		return vertex;
	}
	public int getDegree() {
		return degree;
	}
	
	@Override
	public int compareTo(VertexDegreeComparator other) {
		return this.degree.compareTo(other.degree);
	}
	
}
