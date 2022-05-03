package actions;

import java.io.IOException;

import javax.servlet.ServletException;

import constants.ForwardConst;

/**
 *
 * エラー発生時の処理を行うActionクラス
 *
 */

public class UnknownAction extends ActionBase{

    /**
     * 共通エラー画面「お探しの画面は見つかりませんでした。」を表示
     */
    @Override
    public void process() throws ServletException,IOException{
        //エラー画面を表示
        forward(ForwardConst.FW_ERR_UNKNOWN);
    }
}
