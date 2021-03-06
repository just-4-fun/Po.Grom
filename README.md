Po.Grom - Повідом Громадськість
=======

Повідом Громадськість. Це простий спосіб надати актуальну та корисну інформацію із свого мобільного пристрою, з місця подій.


Что это?

Инструмент для сбора и обработки информационных сообщений, поступающих от граждан. 
Сообщения отправляются пользователями с мобильных устройств (смартфонов, планшетов) через интернет или sms.
Сообщения отображаются в веб-интерфейсе диспетчера, в виде таблицы, и на карте местности.
Сообщение может содержать текст, фотографии, адрес и координаты места отправки, контактную информацию, и другую дополнительную информацию. 
Функция SOS: нажимая кнопку, пользователь активирует периодическую отправку сообщений о чрезвычайной ситуации. Диспетчет получает SOS-сообщение с текущим местоположением и историей перемещений в том же веб-интерфейсе.

Кому это интересно?

Организациям, занимающимся сбором актуальных новостей, событий и фактов с мест их обнаружения. А так же организациям, оказывающим помощь в чрезвычайных ситуациях.

Как это реализовано?

1. Интерфейс диспетчера.

(Пример здесь: https://www.google.com/fusiontables/DataSource?docid=1NLRaoQU1mhqsyyaYLNIuIkML6S0FEZER7Dihssk)
Отображение сообщений реализовано в виде доступного в любом веб-браузере интерфейса на базе Google Fusion Tables (документ Google Drive). 
Интерфейс отображает сообщения в виде таблицы колонок с текстом и мини-фотографиями, которую можно редактировать, производить сортировку, поиск и фильтрацию. А так же в виде карты местности с аналогичными поиском и фильтрацией, на которой отображаются координаты мест отправки сообщений.
Для использования интерфейса диспетчера необходим только почтовый Google аккаунт.
Доступ к данным можно настраивать. Например сделать часть данных публичной и не редактируемой, установив фильтр данных и исключив некоторые колонки таблицы. Получив, например, новую таблицу с доступом для любого посетителя, в которой отображаются только имя, текст сообщения и фотографии, но не отображаются SOS-сообщения. Или таблицу с доступом для втотрого диспетчера, в которой отображаются только SOS-сообщения с именем, контактной информацией и координатами.

2. Мобильное приложение пользователя.

(Пример здесь: https://play.google.com/store/apps/details?id=cyua.android.client)
Сбор сообщений осуществляется приложением, устанавливаемым на мобильное устройство. Установив приложение, пользователь может, находясь в месте события, отправить текстовое сообщение с фотографиями. С сообщением отправляются время, координаты места, контактная информация, и другая информация (например, идентификатор устройства, с которого отправлено сообщение). 
При отсутствии на устройстве в момент отправки сообщения соединения с интернет, сообщение будет отправленно через sms (только текст), если устройство имеет такую возможность. Сообщение поступает на телефон диспетчера, с установленным на него приложением, которое преобразует и пересылает сообщение в общую базу.
На данный момент приложение поддерживает устройства на платформе Android. Приветствуется сотрудничество по написанию версии под iOS и WP.

Себестоимость обслуживания проекта бесплатна до определенного предела объемов передачи данных и хранения фотографий. при превышении этих пределов действуют тарифы:
- для фотографий - тарифы Amazon Simple Storage Service (http://aws.amazon.com/s3/pricing/);
- для серверной части - тарифы Google App Engine (https://cloud.google.com/products/app-engine/).

Любые изменения обсуждаемы. В частности: Название проекта и приложений. Вид интерфейса приложения. Функции приложения, в том числе, дополнительные данные, указываемые пользователем. Вид интерфейса диспетчера, в том числе, названия и расположение колонок таблицы, дополнительные колонки таблицы, вид пиктограмм маркеров на карте, Данные информационного окна при нажатии маркера на карте.
