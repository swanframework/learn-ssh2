java 领域中, 开源jar 包ganymed-ssh2 对ssh2协议进行了实现, 通过ganymed-ssh2 可实现Linux命令: ssh 和 scp 的功能. ganymed-ssh2 提供了多种认证方式, 可通过用户名密码, 秘钥等方式. 笔者常用的是用户名密码方式。 ganymed-ssh2 的开发步骤:
1. 使用主机地址创建连接对象new Connection(host)
2. 创建TCP连接, connection.connect()
3. 进行身份认证，笔者使用的是用户名密码方式: connection.authenticateWithPassword(username, password)
4. 创建会话，每次远程操作都需要重新打开一个会话. connection.openSession()
5. 执行ssh 操作或scp 操作
6. 关闭连接: connection.close()

## 1. 引入maven 依赖
笔者习惯于使用maven 开发, 所以新建maven 项目, 然后引入依赖.  需要注意的是笔者使用的是build210 版本, 是buildxxx系列的最高版本. 不同版本的api 还是有差别的.

```xml
<dependencies>
   <!-- 引入ssh2 依赖 -->
    <dependency>
        <groupId>ch.ethz.ganymed</groupId>
        <artifactId>ganymed-ssh2</artifactId>
        <version>build210</version>
    </dependency>

    <!-- 引入单元测试 -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.12</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 2. 开发工具类

### 2.1 自定义SSH2 工具类
笔者觉得ganymed-ssh2 提供的API 并不符合笔者的开发习惯，所以进行了二次封装。 将常用的ssh2 操作封装成了一个工具类。

```java
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
```

### 2.2 自定义命令返回结果

```java
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
```

## 3. 测试类

```java
public class TestSSH2Util {

    private String host = "127.0.0.1";

    private String username = "root";

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

```