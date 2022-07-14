public class Edit {
    private String uuid;
    private int year;
    private String model;

    public Edit(String uuid, int year, String model) {
        this.uuid = uuid;
        this.year = year;
        this.model = model;
    }

    public String getUuid() {
        return uuid;
    }

    public int getYear() {
        return year;
    }

    public String getModel() {
        return model;
    }
}
