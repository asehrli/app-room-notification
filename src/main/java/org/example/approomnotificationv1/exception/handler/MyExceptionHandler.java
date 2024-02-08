package org.example.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.example.exceptions.NotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler({NotFoundException.class})
    public String handler(Model model,
                          HttpServletRequest req,
                          NotFoundException e) {
        model.addAttribute("mes", e.getMessage());
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("url", req.getRequestURL());
        return "error/404";
    }

}
