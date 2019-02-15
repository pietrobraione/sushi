/**
  * Copyright (c) 2017 Universitat Politècnica de València (UPV)
  * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
  * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
  * 3. Neither the name of the UPV nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  **/
package sbst.runtool;

import java.io.File;
import java.util.List;

public interface ITestingTool
{

	/**
	 * List of additional class path entries required by a testing tool
	 * @return List of directories/jar files
	 */
	public List<File> getExtraClassPath();

	/**
	 * Initialize the testing tool, with details about the code to be tested (SUT)
	 * Called only once.
	 * @param src Directory containing source files of the SUT
	 * @param bin Directory containing class files of the SUT
	 * @param classPath List of directories/jar files (dependencies of the SUT)
	 */
	public void initialize(File src, File bin, List<File> classPath);

	/**
	 * Run the test tool, and let it generate test cases for a given class
	 * @param cName Name of the class for which unit tests should be generated
	 * @param timeBudget How long the tool must run to test the class (in miliseconds)
	 */
	public void run(String cName, long timeBudget);

}