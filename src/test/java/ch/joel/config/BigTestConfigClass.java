package ch.joel.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BigTestConfigClass {

	private String string;
	private boolean bool;
	private int number;
	private double dbl;
	private SmallObject smallObject;

}
