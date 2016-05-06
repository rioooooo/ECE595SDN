import java.util.List;

public class Vertex {
	private Vertex previous;
	private int id;
	private int label;
	private List<Vertex> prev;
	
	public void setId(int id) {
		this.id = id;
	}
 
	
	public int getLabel() {
		return label;
	}
 

	public void setwidth(int label) {
		this.label = label;
	}
 
	
	
	public List<Vertex> getPrev() {
		return prev;
	}
 

	public void setPrev(List<Vertex> prev) {
		this.prev = prev;
	}
	public Vertex getPrevious() {
		return previous;
	}
	public void setPrevious(Vertex previous) {
		this.previous = previous;
	}
	public int getId() {
		return id;
	}
 
}
