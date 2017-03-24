package ru.mail.park.Service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

/**
 * Created by Варя on 24.03.2017.
 */
@RestController
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(@NotNull ServiceService serviceService){this.serviceService = serviceService;}

    @GetMapping("api/service/status")
    public ResponseEntity serviceStatus(){
        return serviceService.serviceStatus();
    }

    @PostMapping("api/service/clear")
    public ResponseEntity serviceClear(){
        return serviceService.serviceClear();
    }
}
