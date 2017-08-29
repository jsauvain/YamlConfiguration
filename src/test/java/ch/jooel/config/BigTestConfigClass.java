package ch.jooel.config;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BigTestConfigClass {

	private String string;
	private boolean bool;
	private int number;
	private double dbl;
	private SmallObject smallObject;

}
