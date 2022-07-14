import java.util.ArrayList;

public class Car {
    private String model;
    private int id;
    private String uuid;
    private int year;
    private String color;
    private ArrayList<Airbags> airbags;
    private String date;
    private int vat;
    private int price;
    private Boolean pdf;

    public Car(String model, int year, String color, ArrayList<Airbags> airbags, String date, int vat, int price) {
        this.model = model;
        this.year = year;
        this.color = color;
        this.airbags = airbags;
        this.date = date;
        this.vat = vat;
        this.price = price;
        this.pdf = false;
    }

    public void setPdf(Boolean pdf) {
        this.pdf = pdf;
    }



    public Car(String model, int year, String color, ArrayList<Airbags> airbags) {
        this.model = model;
        this.year = year;
        this.color = color;
        this.airbags = airbags;
        this.pdf = false;
    }

    public int getVat() {
        return vat;
    }

    public int getPrice() {
        return price;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public String getColor() {
        return color;
    }

    public ArrayList<Airbags> getAirbags() {
        return airbags;
    }

    public String getUuid() {
        return uuid;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setYear(int year) {
        this.year = year;
    }
}