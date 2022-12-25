package Burnie;

public class RequestResult {
    private static final RequestResult INSTANCE = new RequestResult();
    private RequestResult() {}
    public static RequestResult getInstance() {return INSTANCE;}

    private int intData;
    private String stringData;

    public int getIntData() {return intData;}
    public void setIntData(int intData) {this.intData = intData;}

    public String getStringData() {return stringData;}
    public void setStringData(String stringData) {this.stringData = stringData;}
}
