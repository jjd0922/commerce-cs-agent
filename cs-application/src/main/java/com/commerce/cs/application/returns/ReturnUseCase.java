package com.commerce.cs.application.returns;

public interface ReturnUseCase {

    ReturnResult requestReturn(ReturnCommand command);
}
