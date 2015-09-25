Установка необходимого софта для разработки:
[1](http://mindtherobot.com/blog/209/android-beginners-from-bare-windows-to-your-first-app/)
[2](http://mindtherobot.com/blog/452/android-beginners-ndk-setup-step-by-step/)

Далее импортируем проект File->Import->General->Existing Projects into Workspace.

Если после импорта появляется ошибка "Android requires compiler compliance level", выбираем проект правой кнопкой в Package Explorer, "Android Tools" -> "Fix Project Properties".
Далее Project -> Clean.


Если после импорта лезут ошибки "the method must override superclass", открываем Project->Properties->Java Compiler, выставляем "1.6" в настройке "Compiler compliance level".

Не забываем собрать библиотеку с помощью ndk!

**Обязательный шаг:** запустить плеер на эмуляторе, и скопировать из LogCat строку, расположенную сразу над сообщением "package signed with a key other than the debug key". Заменить значение константы DEBUGKEY в файле Utility.java полученной строкой. После этих действий, программа станет верно определять, подписана ли она дебажным или релизным ключом, соотв. в дебажной версии не будет отправлять Crash-Report.


---

Иногда Eclipse Juno отказывается стартовать. Нужно удалить файл:
YOUR\_WORKSPACE/.metadata/.plugins/org.eclipse.core.resources/.snap


---

Небольшой трюк, использовать на свой страх и риск.

SVN+Eclipse: настройка Eclipse так, чтобы он не трогал папки .svn
http://www.damonkohler.com/2009/07/make-eclipse-ignore-svn-directories.html