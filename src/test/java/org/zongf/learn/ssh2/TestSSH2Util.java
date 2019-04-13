package org.zongf.learn.ssh2;

import ch.ethz.ssh2.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @Description: 测试SSH2 工具类
 * @author: zongf
 * @date: 2019-03-25 21:52
 */
public class TestSSH2Util {

    private String host = "127.0.0.1";

    private String username = "zongf";

    private String password = "123456";

    private Connection connection;

    // 执行每个单元测试用例前, 创建ssh2连接
    @Before
    public void setup(){
        connection = SSH2Util.openConnection(host, username, password);
    }

    // 执行每个单元测试后, 关闭连接
    @After
    public void tearDown(){
        connection.close();
    }

    /** 测试连接 */
    @Test
    public void test_connect(){
        System.out.println("hostname:" + connection.getHostname());
    }

    /** 测试执行正确命令 */
    @Test
    public void test_exec_right() {
        ExecCmdResult execCmdResult = SSH2Util.execCommand(connection, "ls /home -l");

        System.out.println("命令是否正确执行:" + execCmdResult.isSuccess());
        System.out.println("命令执行结果:\n" + execCmdResult.getResult());
    }

    /** 测试执行带命令通配符的命令 */
    @Test
    public void test_exec_ms() {
        ExecCmdResult execCmdResult = SSH2Util.execCommand(connection, "ls -d /*bin");

        System.out.println("命令是否正确执行:" + execCmdResult.isSuccess());
        System.out.println("命令执行结果:\n" + execCmdResult.getResult());
    }

    /** 测试执行错误命令 */
    @Test
    public void test_exec_wrong() {
        ExecCmdResult execCmdResult = SSH2Util.execCommand(connection, "ls /2");

        System.out.println("命令是否正确执行:" + execCmdResult.isSuccess());
        System.out.println("命令执行结果:\n" + execCmdResult.getResult());
    }

    /** 测试下载单个文件 */
    @Test
    public void test_download() {
        SSH2Util.download(connection, ".","/etc/passwd");
    }

    /** 测试批量下载多个文件 */
    @Test
    public void test_download_batch(){
        SSH2Util.download(connection, ".", "/etc/passwd", "/bin/bash");
    }

    /** 测试通配符下载多个文件 */
    @Test
    public void test_download_Parttern(){
        SSH2Util.downloadByPattern(connection, ".", "/bin", "*m");
    }

    /** 测试单个上传 */
    @Test
    public void test_upload() {
        SSH2Util.upload(connection, "/tmp/zongf", "passwd");
    }

    /** 测试批量上传 */
    @Test
    public void test_upload_batch() {
        SSH2Util.upload(connection, "/tmp/zongf/tt", "rm", "udevadm");
    }
}
