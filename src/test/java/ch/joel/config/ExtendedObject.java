package ch.joel.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ExtendedObject extends SmallObject {

	private String password;

}
