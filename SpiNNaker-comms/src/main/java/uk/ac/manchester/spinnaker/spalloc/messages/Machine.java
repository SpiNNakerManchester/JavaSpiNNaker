package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.Collections;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * Describes a machine by its name, tags, width and height.
 */
public class Machine {

	private String name;
	private List<String> tags = emptyList();
	private int width;
	private int height;
	private List<BoardCoordinates> deadBoards = Collections.emptyList();
    private List<BoardLink> deadLinks = Collections.emptyList();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags == null ? emptyList() : unmodifiableList(tags);
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * @return the deadBoards
	 */
	public List<BoardCoordinates> getDeadBoards() {
		return deadBoards;
	}

	/**
	 * @param deadBoards
	 *            the deadBoards to set
	 */
	public void setDeadBoards(List<BoardCoordinates> deadBoards) {
		this.deadBoards = deadBoards;
	}

    /**
     * @return the deadLinks
     */
    public List<BoardLink> getDeadLinks() {
        return deadLinks;
    }

    /**
     * @param deadLinks the deadLinks to set
     */
    public void setDeadLinks(List<BoardLink> deadLinks) {
        this.deadLinks = deadLinks;
    }
}
