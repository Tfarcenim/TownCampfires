package tfar.towncampfires.config;

public record IntegerRange(int min,int max) {

    public static IntegerRange inclusive(int min,int max) {
        return new IntegerRange(min,max);
    }
}
