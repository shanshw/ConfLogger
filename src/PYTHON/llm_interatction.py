import argparse
import os
import re
import json
import random
import subprocess
import tempfile
import openai


OPENAI_KEY=""


def interact_with_llm(user_prompt,outputdir):

    system_ins=( 
"You are an experienced developer. You are an expert in configuration errors, configuraiton bugs and logging practice. "
    )
    dirname=outputdir

    print(system_ins)
    
    user_prompt = """
    You are provided with configuration checking code, tagged with <code-whole> and <code-specific>. The former indicates a comprehensive view of code snippet and the latter indicates the scope for you to insert logging code. Tag <param> indicates the related configuration parameter key of the scope, `null` stands for not tracking rather than not existing.
    The code snippet of the scope can be categorized into three types:
    The first type, to check if a parameter is properly-set, if not, the code handles the cases. The second type, the checking code as a whole is about enabling a service or not. In the case, take the code itself as a whole. The third type, the checking code is configuring the behavior of systems based on the configuration parameter value. 
    """
    "Please finish two tasks based on the input and the following instructions."
    """
    The first task is to inspect existing outputs and determine to skip them or to comment them out. The second task is to determine the log insertion position and generate the inserted logging code. 
    For the first task, please strictly follow the rules:
    1. Firstly, inspect existing system outputs related to the scope. These system outputs can be logs, error messages, console outputs, thrown exceptions. When localize these outputs, please check their content. If these outputs contain configuration parameters, such as the configuraiton parameter keys, the parameter values, the unsatisifed configuration-related conditions, comment them with a comment: "// ConfLogger Skipped: <reason>". You should fill in the <reason> to explain why skip the existing system outputs. In such case, don't insert logging code in the position. 
    2. If these outputs lack the information of configuration setting and fail to guide users to a proper configuration settings,COMMENT THEM OUT and then mark with an extra comment:"// ConfLogger Commented:<reason>". You should also fill in the <reason> to explain why comment the existing system outputs out.
    NOTICE, DON'T COMMENT THE THROWN EXCEPTIONS OUT. If they don't satisfy the requirements, just add another log position before the thrown exceptions.
    """
    """
    For the second task: determine the log insertion position and generate configuration-related logging code, please strictly follow the rules:
    1. *IMPORTANT* Figure out the type of the identified scope and then determine the log position to insert the configuration-related logging code based on the type.
    The log position identification is a path constraints summarization task.
    For type 1, the log positions include path to handling the wrongly-set or unset values.
    For type 2, the log positions include both the service-enable and service-disable path. If there are more constraints on enabling or disabling a service, the log positions are multiple.
    For type 3, different configuration parameter value result in different path constraints, thus the log positions are multiple.
    Comment on the log positions with "ConfLogger Inserted Start" and "ConfLogger Inserted End". 
    Remember to skip or comment out the repeated existing system output positions identified from task 1.
    2. The logging code contains both logging statements and simple logical operations (based on the determined log position). For example, insert a `else-if` or `else` block with logging code to indicate the failure to enable some service or mistakes on setting configuration parameters.
    3. For logging statements, determine the log level in five types only: trace, debug, info, warn and error.
    4. *IMPORTANT* For logging statements, the static text should expose configurtion-related execution information as much as possible: 
    a. The constraints of the checks. For example, operational dependencies, computational relationships and more.
    b. Guides and highlights on proper configuration setting. 
    For type 1, please expose the execution information on how the system handle misconfiguration. 
    For type 2, if a service fails to activate, please offer suggestions on how to properly activate it.
    For type 3, please output the information on how do the configuration parameters impact the system behavior. 
    If the given code exposes any operations on the related configuration-related variables, demonstrate the operations. For example, if there are arithmetic operations, output the arithmetic relations to expose more configuration-related execution information.
    c. If there are multiple scopes identified, please consider their inner relations based on the whole code snippet. For example, their priority on determining some service or functionality. Highlight their inner antagonistic- or synergistic-effect. NOTICE, they can be totally irrelated, if they are irrelated, ignore the illustration of their relations.
    d. If the control flow is non-trivival, please explain the reason why the control flow reaches the block. For example, in the nested if-else block, output the outer constraints if necessary.
    5. For logging statements, the dynamic variable should:
    a. contain the related configuration parameter value and the related configuration parameter name (key). DON'T hard-code the configuration parameter key if you can find its constant or variable in the whole code snippet. Only hard-code the attached constant string with tag <param> if no context indicates the accordingly variable or constant. If you cannot identify the parameter key, don't output it. Please carefully distinguish configuration value and configuration name. 
    b. If necessary, include more other variables to expose more configuration-related execution information.
    6. Generate the logging statements using the following format in java log4j: `logger.xxx();` 
    7.  Please only insert the generated logging code (task 2) on the identified log insertion position. 
    Don't forget to comment on the log position with "ConfLogger Inserted Start" and "ConfLogger Inserted End". 
    Think step by step, your work helps a lot.
    Please just output the updated version of the whole code. 
    """ + "\t"+user_prompt

    result = openai.chat.completions.create(model="gpt-4o-2024-08-06",
                                            messages=[{"role": "system", "content": system_ins},
                                                      {"role": "user", "content": user_prompt}],temperature=0)
    
    
    
    strd = result.choices[0].message.content #message.content
    prompt_tokens=result.usage.prompt_tokens
    completion_tokens=result.usage.completion_tokens
    total_tokens=result.usage.total_tokens
    
    usage="prompt: "+str(prompt_tokens)+"\t completion:"+str(completion_tokens)+"\t total:"+str(total_tokens)

    if not os.path.exists(dirname):
        os.mkdir(dirname)

    write_response(os.path.join(dirname,'user_prompt'),user_prompt)
    write_response(os.path.join(dirname,'response.out'),strd)
    write_response(os.path.join(dirname,'response_tokens_overhead.out'),usage)


    return strd

def write_response(filename,content):
    with open(filename,'w') as file:
        for line in content:
            file.write(line)
    print('file '+filename+' has finished writing')

def main(us,outputdir):
    interact_with_llm(us,outputdir)
    # print("Call Done")

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description='Process some inputs.')
    parser.add_argument('-p', '--prompt', type=str, required=True, help='User prompt')
    parser.add_argument('-d', '--dir', type=str, required=True, help='output directory')
    args = parser.parse_args()
    user_prompt = args.prompt
    outputdir = args.dir
    main(user_prompt,outputdir)