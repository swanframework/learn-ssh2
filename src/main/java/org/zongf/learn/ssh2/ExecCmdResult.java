package org.zongf.learn.ssh2;

/**
 * @Description: 命令返回结果
 * @author: zongf
 * @date: 2019-03-25 21:03
 */
public class ExecCmdResult {

    // 命令执行是否成功
    private boolean success;

    // 输出结果
    private String result;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
