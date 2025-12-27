# AI Usage

I only used AI to debug a null pointer exception I was getting. I knew where it was but I didn't fully understandy why. The answer was actually pretty simple and I didn't see it because I was tired. Basically I forgot to add a null check in the toResponse function in the controllers when getting book.getLoanedTo().
