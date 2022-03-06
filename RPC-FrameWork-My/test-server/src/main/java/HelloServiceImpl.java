import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import annotation.Service;


//@Service
public class HelloServiceImpl implements HelloService {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(HelloObject object) {
        logger.info("服务端接收到：{}", object);
        return "服务端调用后返回值id=" + object;
//        return "使用HelloServiceImpl1方法";
    }

}
