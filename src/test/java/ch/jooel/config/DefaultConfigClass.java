package ch.jooel.config;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class DefaultConfigClass {

	private int number = 5;
	private String string = "Hello World";
	private boolean bool = true;
	private long longNumber;

}
