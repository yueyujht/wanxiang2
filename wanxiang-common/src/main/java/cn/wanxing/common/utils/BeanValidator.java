package cn.wanxing.common.utils;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;

import java.util.Set;


/**
 * 参数校验工具
 */
public class BeanValidator {

    // failFast：一旦遇到参数错误则抛出异常，不会对后面的参数进行校验
    private static Validator validator = Validation.byProvider(HibernateValidator.class).configure().failFast(true)
            .buildValidatorFactory().getValidator();

    /**
     * @param object object
     * @param groups groups
     */
    public static void validateObject(Object object, Class<?>... groups) throws ValidationException {
        // 有参数校验不通过，则会往constraintViolations添加信息
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        // constraintViolations有信息则此次参数校验不通过
        if (constraintViolations.stream().findFirst().isPresent()) {
            throw new ValidationException(constraintViolations.stream().findFirst().get().getMessage());
        }
    }
}