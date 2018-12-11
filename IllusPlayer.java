import java.net.InetAddress;
/*
stores and displays player's information
such as name and score
*/
public class IllusPlayer {
    private String name;
    private String id;
    private int score;
    private InetAddress address;
    private int port;
    private String givenName;

    /*
    setters and getters for 'givenName'
    */
    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    /*
    setters and getters for 'addresss'
    */
    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    /*
    setters and getters for 'port'
    */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /*
    setters and getters for 'name'
    */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /*
    setters and getters for 'name'
    */
    public String getId() {
        return id;
    }

    /*
    setters and getters for 'id'
    */
    public void setId(String id) {
        this.id = id;
    }

    /*
    setters and getters for 'score'
    */
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
