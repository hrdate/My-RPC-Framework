import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import annotation.Service;


//@Service
public class HelloServiceImpl implements HelloService {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(HelloObject object) {
        logger.info("接收到消息：{}", object.getMessage());
        return "使用HelloServiceImpl1方法";
    }

}
