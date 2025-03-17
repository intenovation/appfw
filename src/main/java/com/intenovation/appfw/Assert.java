package com.intenovation.appfw;

import java.util.Collection;

public class Assert {

	public static void notNull(Object o, String errorMessage) {
		if (o == null)
			throw new AssertionException(errorMessage);

	}

	public static void notEmpty(Collection col, String errorMessage) {
		if (col == null || col.isEmpty())
			throw new AssertionException(errorMessage);

	}

	public static void not(boolean bool, String errorMessage) {
		if (bool)
			throw new AssertionException(errorMessage);

	}

}
