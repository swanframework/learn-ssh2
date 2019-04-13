package org.zongf.learn.ssh2;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;

import java.io.*;

/**
 * @Description: ssh2 工具类
 * @author: zongf
 * @date: 2019-03-25 20:23
 */
public class SSH2Util {

    private static String DEFAULT_CHARSET = "UTF-8";


    /**
     * @Description: 建立ssh2连接
     * @param host 主机地址
     * @param username 用户名
     * @param password 密码
     * @return: Connection
     * @author: zongf
     * @time: 2019-03-25 20:46:32
     */
    public static Connection openConnection(String host, String username, String password) {
        Connection connection;

        try {
            connection = new Connection(host);

            // 建立ssh2 连接
            connection.connect();

            // 校验用户名密码
            boolean login = connection.authenticateWithPassword(username, password);

            // 登录成功返回连接
            if (login) {
                return connection;
            }else {
                throw new RuntimeException("用户名密码不正确");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @Description: 执行一条命令
     * @param connection ssh2 连接对象
     * @return: ExecCmdResult 执行结果, 封装执行状态和执行结果
     * @author: zongf
     * @time: 2019-03-25 20:47:14
     */
    public static ExecCmdResult execCommand(Connection connection, String command) {

        ExecCmdResult execCmdResult = new ExecCmdResult();

        Session session = null;

        try {
            session = connection.openSession();

            // 执行命令
            session.execCommand(command);

            // 解析结果
            String result = parseResult(session.getStdout());

            // 若解析结果为空, 则表示执行命令发生了错误, 尝试解析错误输出
            if (result.isEmpty()) {
                result = parseResult(session.getStderr());
            }else {
                execCmdResult.setSuccess(true);
            }

            // 设置响应结果
            execCmdResult.setResult(result);

            return execCmdResult;

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (session != null) {
                session.close();
            }
        }
        return null;
    }

    /**
     * @Description: 下载文件,只能下载文件类型, 如果是目录则抛出异常
     * @param connection ssh2 连接对象
     * @param remoteFilePathAbs 远程文件绝对路径名
     * @param localDir 本地文件夹
     * @author: zongf
     * @time: 2019-03-25 21:41:33
     */
    public static void download(Connection connection, String localDir, String... remoteFilePathAbs) {

        // 如果传参为空, 则返回
        if(remoteFilePathAbs == null) return;

        SCPClient scpClient = new SCPClient(connection);

        try {
            scpClient.get(remoteFilePathAbs, localDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description: 通配符方式下载文件
     * @param connection ssh2 连接对象
     * @param remoteDirAbsPath 远程文件绝对路径
     * @param fileNamePattern 文件名通配符匹配模式
     * @param localDir 本地目录, 可接受相对绝对路径
     * @author: zongf
     * @time: 2019-03-25 22:20:07
     */
    public static void downloadByPattern(Connection connection, String localDir, String remoteDirAbsPath, String fileNamePattern) {

        ExecCmdResult execCmdResult = execCommand(connection, "ls " + remoteDirAbsPath + "/" + fileNamePattern);

        if (execCmdResult.isSuccess()) {

            String[] files = execCmdResult.getResult().split("\n");

            SCPClient scpClient = new SCPClient(connection);

            try {
                scpClient.get(files, localDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @Description: 上传文件至远程服务器. 本地文件不存在时, 抛出异常,但会上传一个0字节的文件; 远程目录不存在时, 抛出异常.
     * @param connection ssh2 连接对象
     * @param remoteDirAbsPath 远程服务器文件夹, 必须是绝对路径
     * @param localFile 本地文件列表, 可接受绝对路径相对路径参数
     * @author: zongf
     * @time:  2018-03-25 21:44:07
     */
    public static void upload(Connection connection, String remoteDirAbsPath, String... localFile) {

        // 如果传参为空, 则返回
        if(localFile == null) return;

        SCPClient scpClient = new SCPClient(connection);

        try {
            scpClient.put(localFile, remoteDirAbsPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description: 解析命令结果
     * @param inputStream 输入流
     * @return: String 字符串
     * @author: zongf
     * @time:  2019-03-25 21:06:23
     */
    private static String parseResult(InputStream inputStream) throws IOException {
        // 读取输出流内容
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET));
        StringBuffer resultSb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            resultSb.append(line + "\n");
        }
        return resultSb.toString();
    }
}


