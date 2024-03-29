package services;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.NoResultException;

import actions.views.EmployeeConverter;
import actions.views.EmployeeView;
import constants.JpaConst;
import models.Employee;
import models.validators.EmployeeValidator;
import utils.EncryptUtil;

/**
 *
 * 従業員テーブルの操作に関わる処理を行うクラス
 *
 */
public class EmployeeService extends ServiceBase{

    /**
     * 指定されたページ数の一覧画面に表示するデータを取得し、EmployeeViewのリストで返却
     * @param page ページ数
     * @return 表示するデータのリスト
     */
    public List<EmployeeView> getPerPage(int page){
        List<Employee> employees = em.createNamedQuery(JpaConst.Q_EMP_GET_ALL, Employee.class)
                .setFirstResult(JpaConst.ROW_PER_PAGE * (page - 1))
                .setMaxResults(JpaConst.ROW_PER_PAGE)
                .getResultList();

        return EmployeeConverter.toViewList(employees);
    }

    /**
     * 従業員テーブルのデータの件数を取得し、返却
     * @return 従業員テーブルのデータの件数
     */
    public long countAll() {
        long empCount = (long)em.createNamedQuery(JpaConst.Q_EMP_COUNT, Long.class)
                .getSingleResult();

        return empCount;
    }

    /**
     * 社員番号、パスワードを条件に取得したデータをEmployeeViewのインスタンスで取得
     * @param code 社員番号
     * @param plainPass パスワード文字列
     * @param pepper pepper文字列
     * @return 取得データのインスタンス　取得できない場合null
     */
    public EmployeeView findOne(String code, String plainpass, String pepper) {
        Employee e = null;
        try {
            //パスワードのハッシュ化
            String pass = EncryptUtil.getPasswordEncrypt(plainpass, pepper);

            //社員番号とハッシュ化済パスワードを条件に未削除の従業員を1件取得
            e = em.createNamedQuery(JpaConst.Q_EMP_GET_BY_CODE_AND_PASS, Employee.class)
                    .setParameter(JpaConst.JPQL_PARM_CODE, code)
                    .setParameter(JpaConst.JPQL_PARM_PASSWORD, pass)
                    .getSingleResult();
        }catch(NoResultException ex) {

        }

        return EmployeeConverter.toView(e);
    }

    /**
     * idを条件に取得したデータをEmployeeViewのインスタンスで返却
     * @param id
     * @return 取得したデータのインスタンス
     */
    public EmployeeView findOne(int id) {
        Employee e = findOneInternal(id);
        return EmployeeConverter.toView(e);
    }

    /**
     * 社員番号を条件に該当するデータの件数を取得し、返却
     * @param code 社員番号
     * @return 該当するデータの件数
     */
    public long countByCode(String code) {
        //指定した社員番号を保持する従業員の件数を取得
        long employees_count = (long)em.createNamedQuery(JpaConst.Q_EMP_COUNT_RESISTERD_BY_CODE, Long.class)
                .setParameter(JpaConst.JPQL_PARM_CODE, code)
                .getSingleResult();
        return employees_count;
    }

    /**
     * 画面から入力された従業員の登録内容を元にデータを1件作成し、従業員テーブルに登録
     * @param ev 画面から入力された従業員の登録内容
     * @param pepper pepper文字列
     * @return バリエーションや登録処理中に発生したエラーのリスト
     */
    public List<String> create(EmployeeView ev, String pepper){
        //パスワードをハッシュ化して設定
        String pass = EncryptUtil.getPasswordEncrypt(ev.getPassword(), pepper);
        ev.setPassword(pass);

        //登録日時、更新日時は現在時刻を設定
        LocalDateTime now = LocalDateTime.now();
        ev.setCreatedAt(now);
        ev.setUpdatedAt(now);

        //登録内容のバリエーションを行う
        List<String> errors = EmployeeValidator.validate(this, ev, true, true);

        //バリエーションがなければデータを登録
        if(errors.size() == 0) {
            create(ev);
        }
        return errors;
    }

    /**
     * 画面から入力された従業員の更新内容を元にデータを1件作成し、従業員テーブルを更新
     * @param ev 画面から入力された従業員の登録内容
     * @param pepper pepper文字列
     * @return バリエーションや更新処理中に発生したエラーのリスト
     */
    public List<String> update(EmployeeView ev, String pepper){
        //idを条件に登録済みの従業員情報を取得
        EmployeeView savedEmp = findOne(ev.getId());

        boolean validateCode = false;
        if(!savedEmp.getCode().equals(ev.getCode())) {
            //社員番号を更新する場合

            //社員番号についてのバリエーションを行う
            validateCode = true;
            //変更後の社員番号を設定
            savedEmp.setCode(ev.getCode());
        }

        boolean validatePass = false;
        if(ev.getPassword() != null && !ev.getPassword().equals("")) {
            //パスワードに入力がある場合

            //パスワードについてのバリエーションを行う
            validatePass = true;

            //変更後のパスワードをハッシュ化し設定
            savedEmp.setPassword(
                    EncryptUtil.getPasswordEncrypt(ev.getPassword(), pepper));
        }

        savedEmp.setName(ev.getName());   //変更後の氏名を設定
        savedEmp.setAdminFlag(ev.getAdminFlag());  //変更後の管理者フラグを設定

        //更新日時に現在時刻を設定
        LocalDateTime today = LocalDateTime.now();
        savedEmp.setUpdatedAt(today);

        //更新内容についてバリエーションを行う
        List<String> errors = EmployeeValidator.validate(this, savedEmp, validateCode, validatePass);

        //バリエーションエラーがなければデータを更新
        if(errors.size() == 0) {
            update(savedEmp);
        }

        return errors;
    }

    /**
     * idを条件に従業員データを論理削除する
     * @param id
     */
    public void destroy(Integer id) {
        //idを条件に登録済みの従業員情報を取得する
        EmployeeView savedEmp = findOne(id);

      //更新日時に現在時刻を設定
        LocalDateTime today = LocalDateTime.now();
        savedEmp.setUpdatedAt(today);

        //論理削除フラグを立てる
        savedEmp.setDeleteFlag(JpaConst.EMP_DEL_TRUE);

        //更新処理を行う
        update(savedEmp);
    }

    /**
     * 社員番号とパスワードを条件に検索し、データが取得できるかどうかで認証結果を返却
     * @param code 社員番号
     * @param plainPass パスワード
     * @param pepper pepper文字列
     * @return 認証結果を返す(成功:true,失敗:false)
     */
    public Boolean validateLogin(String code, String plainPass, String pepper) {

        boolean isValidEmployee = false;
        if(code != null && !code.equals("") && plainPass != null && !plainPass.equals("")) {
            EmployeeView ev = findOne(code, plainPass, pepper);

            if(ev != null && ev.getId() != null) {
                //データが取得できた場合、認証成功
                isValidEmployee = true;
            }
        }

        //認証結果を返却
        return isValidEmployee;
    }

    /**
     * idを条件にデータを1権取得し、Employeeのインスタンスで返却
     * @param id
     * @return 取得データのインスタンス
     */
    private Employee findOneInternal(int id) {
        Employee e = em.find(Employee.class, id);

        return e;
    }

    /**
     * 従業員データを1件登録する
     * @param ev 従業員データ
     * @return 登録結果(成功:true,失敗:false)
     */
    private void create(EmployeeView ev) {
        em.getTransaction().begin();
        em.persist(EmployeeConverter.toModel(ev));
        em.getTransaction().commit();
    }

    /**
     * 従業員データを更新する
     * @param ev 画面から入力された従業員の登録内容
     */
    private void update(EmployeeView ev) {
        em.getTransaction().begin();
        Employee e = findOneInternal(ev.getId());
        EmployeeConverter.copyViewToModel(e, ev);
        em.getTransaction().commit();
    }
}
