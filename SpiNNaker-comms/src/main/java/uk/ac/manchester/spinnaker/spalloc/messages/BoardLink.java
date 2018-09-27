package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A combination of x, y,z and a Link.
 *
 * @author Christian
 */
@JsonPropertyOrder({
		"x", "y", "z", "link"
})
@JsonFormat(shape = ARRAY)
public class BoardLink {
    // TODO verify format and meaning.

    private int x;
    private int y;
    private int z;
    private int link;

    /**
     * Empty constructor for unmarshaller.
     */
    public BoardLink() {
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * @return the z
     */
    public int getZ() {
        return z;
    }

    /**
     * @param z the z to set
     */
    public void setZ(int z) {
        this.z = z;
    }

    /**
     * @return the link
     */
    public int getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(int link) {
        this.link = link;
    }
}
