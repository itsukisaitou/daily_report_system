package constants;

//アプリケーションスコープのパラメータ名を定義するEnumクラス

public enum PropertyConst {

    //ペッパー文字列
    PEPPER("pepper");

    private final String text;
    private PropertyConst(final String text) {
        this.text = text;
    }

    //値取得
    public String getValue() {
        return this.text;
    }
}
