package service;

import api.ByeService;
import core.annotation.Service;

@Service
public class ByeServiceImpl implements ByeService {
    @Override
    public String bye(String name) {
            return "bye: " + name;
    }
}
