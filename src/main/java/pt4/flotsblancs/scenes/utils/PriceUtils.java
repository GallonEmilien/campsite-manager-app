package pt4.flotsblancs.scenes.utils;

public class PriceUtils {
    
    public static String priceToString(int price){
        float res = price/10;
        if(res % 1==0){
            return res/10+"0";
        }
        return res/10+"";
    }
}
